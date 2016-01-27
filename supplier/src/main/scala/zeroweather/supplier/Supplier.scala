package zeroweather.supplier

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import org.zeromq.ZMQ
import zeroweather.message.WeatherRequested

import scala.annotation.tailrec
import org.velvia.msgpack._

trait Service {
  val logger = Logger(LoggerFactory.getLogger(classOf[Service]))
  val context: ZMQ.Context
  val endpoint: String
  val weatherSourceConnector: WeatherSourceConnector

  lazy val socket = {
    val s = context.socket(ZMQ.REP)
    logger.info(s"Binding server to ${endpoint}")
    s.bind(endpoint)
    s
  }

  @tailrec
  final def handleMessage(loop: Boolean = true): Unit = {
    val msg = socket.recv()
    val req = unpack[WeatherRequested](msg)
    val res = weatherSourceConnector.fetchWeather(req)

    logger.debug(s"${req} -> ${res}")

    socket.send(pack(res))

    if (loop) {
      handleMessage(loop)
    }
  }

}

object Supplier extends App with Service with WeatherSource with Configuration {
  override val context = ZMQ.context(config.getInt("zeromq.ioThreads"))
  override val endpoint = config.getString("zeromq.endpoint")

  handleMessage()
}
