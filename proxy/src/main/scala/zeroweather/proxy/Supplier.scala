package zeroweather.proxy

import java.util.Base64

import akka.actor._
import akka.pattern.ask
import akka.util.{ ByteString, Timeout }
import com.ibm.spark.communication.ZMQMessage
import com.ibm.spark.communication.actors.DealerSocketActor
import org.velvia.msgpack._
import zeroweather.message.{ Weather, WeatherRequested }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait SupplierConnector {
  def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]]
}

class ZeroMQSupplierConnector(system: ActorSystem, val endpoint: String) extends SupplierConnector {

  implicit val timeout = Timeout(2 seconds)

  override def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]] = {
    val dealer = system.actorOf(Props(classOf[DealerActor], endpoint))

    (dealer ? WeatherRequested(countryCode, city)).mapTo[Either[String, Weather]]
  }

}

class DealerActor(endpoint: String) extends Actor with ActorLogging {
  val dealerSocket = context.actorOf(Props(classOf[DealerSocketActor], endpoint, self))

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

      dealerSocket ! ZMQMessage(payload)

      context.become(awaitingResponse(sender, wr))
    }
  }

  def awaitingResponse(client: ActorRef, weatherRequested: WeatherRequested): Receive = {
    case response: ZMQMessage => {
      val payload = response.frames(0)

      //FIXME: Remove BASE64 encoding & add proper String conversion or send in binary format (msgpack)
      val weather = {
        val base64 = Base64.getDecoder.decode(payload.toArray)
        unpack[Weather](base64)
      }

      log.debug("Weather received: {}", weather)

      client ! Right(weather)
      self ! PoisonPill
    }
  }

}

trait Supplier {
  this: Configuration =>

  val system: ActorSystem

  lazy val supplierConnector: SupplierConnector = new ZeroMQSupplierConnector(system, config.getString("zeromq.endpoint"))

}