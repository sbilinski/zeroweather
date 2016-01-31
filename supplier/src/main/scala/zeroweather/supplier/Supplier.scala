package zeroweather.supplier

import java.util.Base64

import akka.actor._
import akka.util.ByteString
import com.ibm.spark.communication.ZMQMessage
import com.ibm.spark.communication.actors.RouterSocketActor
import org.velvia.msgpack._
import zeroweather.message.WeatherRequested

import scala.language.postfixOps

class RouterActor(weatherSourceConnector: WeatherSourceConnector) extends Actor with ActorLogging {
  log.info("Initializing router for {}", weatherSourceConnector.getClass.getSimpleName)
  def receive = {
    case m: ZMQMessage => {
      val worker = context.actorOf(Props(classOf[WorkerActor], sender, weatherSourceConnector))
      worker ! m
    }
  }
}

class WorkerActor(clientSocket: ActorRef, weatherSourceConnector: WeatherSourceConnector) extends Actor with ActorLogging {
  def receive = {
    case m: ZMQMessage => {
      val identity = m.frame(0)
      val payload = m.frame(1)

      //FIXME: Remove BASE64 encoding & add proper String conversion or send in binary format (msgpack)
      val request = {
        val base64 = Base64.getDecoder.decode(payload.toArray)
        unpack[WeatherRequested](base64)
      }

      val weather = weatherSourceConnector.fetchWeather(request)
      log.debug("Mapped {} to {}", request, weather)

      //FIXME: Remove BASE64 encoding & add proper String conversion or send in binary format (msgpack)
      val response = {
        val serialized = pack(weather)
        val base64 = Base64.getEncoder.encode(serialized)
        ByteString.fromArray(base64)
      }

      clientSocket ! ZMQMessage(identity, response)
      self ! PoisonPill
    }
  }
}

trait SupplierService {
  this: WeatherSource with Configuration =>

  val system: ActorSystem

  val router = system.actorOf(Props(classOf[RouterActor], weatherSourceConnector))
  val routerSocket = system.actorOf(Props(classOf[RouterSocketActor], config.getString("zeromq.endpoint"), router, None))
}

object Supplier extends App with SupplierService with WeatherSource with Configuration {
  implicit val system = ActorSystem("Supplier")
}
