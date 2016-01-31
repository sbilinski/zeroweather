package zeroweather.proxy

import java.util.Base64

import akka.actor._
import akka.pattern.ask
import akka.util.{ ByteString, Timeout }
import com.ibm.spark.communication.ZMQMessage
import com.ibm.spark.communication.actors.DealerSocketActor
import org.velvia.msgpack._
import zeroweather.message.{ Weather, WeatherRequested }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait SupplierConnector {
  def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]]
}

class ZeroMQSupplierConnector(system: ActorSystem, val endpoints: Seq[String]) extends SupplierConnector {
  require(!endpoints.isEmpty, "endpoint list must not be empty")

  implicit val fetchTimeout = Timeout(2 seconds)

  override def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]] = {
    val dealer = system.actorOf(Props(classOf[DealerActor], endpoints))

    (dealer ? WeatherRequested(countryCode, city)).mapTo[Either[String, Weather]]
  }

}

class DealerActor(endpoints: Seq[String]) extends Actor with ActorLogging {
  require(!endpoints.isEmpty, "endpoint list must not be empty")

  implicit val executor = context.system.dispatcher

  val endpointTimeout = 500 milliseconds

  def receive = awaitingRequest

  def awaitingRequest: Receive = {
    case wr: WeatherRequested => {
      log.debug("Weather requested: {}", wr)

      //FIXME: Remove BASE64 encoding & add proper String conversion or send in binary format (msgpack)
      val payload = {
        val serialized = pack(wr)
        val base64 = Base64.getEncoder().encode(serialized)
        ByteString.fromArray(base64)
      }

      //TODO: Model 1 Freelance pattern is REQ-REP based. Consider reverting Dealer-Router back to these socket types or move on to Model 3 (router-router).
      val endpoint = roundRobin(endpoints)
      val socket = endpoint match {
        case Some(address) => serverSocket(address)
        case None => throw new AssertionError("Actor must be initialized with a non-empty endpoint list")
      }
      val request = ZMQMessage(payload)

      socket ! request

      val timeoutCancel = context.system.scheduler.schedule(endpointTimeout, endpointTimeout, self, "endpoint timeout")
      context.become(awaitingResponse(sender, request, socket, endpoints.filterNot(_ == endpoint), timeoutCancel))
    }
  }

  def awaitingResponse(client: ActorRef, request: ZMQMessage, socket: ActorRef, remainingEndpoints: Seq[String], timeoutCancel: Cancellable): Receive = {
    case response: ZMQMessage => {
      val payload = response.frames(0)

      //FIXME: Remove BASE64 encoding & add proper String conversion or send in binary format (msgpack)
      val weather = {
        val base64 = Base64.getDecoder.decode(payload.toArray)
        unpack[Weather](base64)
      }

      log.debug("Weather received: {}", weather)

      client ! Right(weather)

      timeoutCancel.cancel()
      self ! PoisonPill
    }
    case "endpoint timeout" => {
      roundRobin(remainingEndpoints) match {
        case None => {
          log.debug("Socket timeout received. No more endpoints available.")

          //TODO: Consider waiting anyway. In this scenario/model we can expect a timeout from the ZMQ socket itself.
          client ! Left(s"All ${endpoints.size} endpoints seem to be down.")

          timeoutCancel.cancel()
          self ! PoisonPill
        }
        case Some(newEndpoint) => {
          log.debug("Socket timeout received. Failover to {}", newEndpoint)
          val newSocket = serverSocket(newEndpoint)
          newSocket ! request
          socket ! PoisonPill
          context.become(awaitingResponse(client, request, newSocket, remainingEndpoints.filterNot(_ == newEndpoint), timeoutCancel))
        }
      }
    }
  }

  private def roundRobin(choices: Seq[String]): Option[String] = choices.headOption

  //TODO: Consider and initial shift of the list and using roundRobin from there
  //private def randomRobin(choices: Seq[String]): Option[String] = choices match {
  //  case Nil => None
  //  case list => Some(list(scala.util.Random.nextInt(list.size)))
  //}

  private def serverSocket(endpoint: String): ActorRef = {
    context.actorOf(Props(classOf[DealerSocketActor], endpoint, self))
  }
}

trait Supplier {
  this: Configuration =>

  val system: ActorSystem

  lazy val supplierConnector: SupplierConnector = new ZeroMQSupplierConnector(system, config.getStringList("zeromq.endpoints").asScala)

}