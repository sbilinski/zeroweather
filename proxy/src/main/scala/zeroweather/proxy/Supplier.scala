package zeroweather.proxy

import org.zeromq.ZMQ
import org.zeromq.ZMQ.{ Poller, PollItem }
import zeroweather.message.{ WeatherRequested, Weather }

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.duration._
import scala.language.postfixOps
import org.velvia.msgpack._

trait SupplierConnector {
  def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]]
}

/**
 * A Scala implementation of the lazy-pirate pattern (based on http://zguide.zeromq.org/java:lpclient)
 */
trait ZeroMQLazyPirate {
  private type Interrupted = Boolean

  val context: ZMQ.Context
  val endpoint: String
  val requestTimeout = 2500 milliseconds

  def send(request: Array[Byte], retries: Int = 5): Array[Byte] = {
    lazy val socket = {
      val s = context.socket(ZMQ.REQ)
      s.connect(endpoint)
      s
    }

    try {
      sendAndPoll(socket, request, retries)
    } finally {
      socket.close()
    }
  }

  @tailrec
  private def sendAndPoll(socket: ZMQ.Socket, request: Array[Byte], retriesLeft: Int): Array[Byte] = {
    if (retriesLeft >= 0 && !Thread.currentThread().isInterrupted) {
      socket.send(request)
      poll(socket) match {
        case (_, Some(response)) => response
        case (true, None) => sendAndPoll(socket, request, retriesLeft)
        case (false, None) => {
          //TODO: Reopen socket?
          sendAndPoll(socket, request, retriesLeft - 1)
        }
      }
    } else {
      throw new java.io.IOException("Failed to process request. Retry limit exhausted.")
    }
  }

  private def poll(socket: ZMQ.Socket): (Interrupted, Option[Array[Byte]]) = {
    val pollItem = new PollItem(socket, Poller.POLLIN)
    ZMQ.poll(Array(pollItem), requestTimeout.toMillis) match {
      case -1 => {
        //Interrupted
        (true, None)
      }
      case _ => {
        if (pollItem.isReadable) {
          val response = socket.recv()
          if (response == null) {
            //Interrupted
            (true, None)
          } else {
            //TODO: Check if message sequence matches?
            (false, Some(response))
          }
        } else {
          (false, None)
        }

      }
    }
  }

}

class ZeroMQSupplierConnector(ioThreads: Int, val endpoint: String)(implicit executor: ExecutionContextExecutor) extends SupplierConnector with ZeroMQLazyPirate {

  val context = ZMQ.context(ioThreads)

  override def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]] = Future {
    val request = pack(WeatherRequested(countryCode, city))
    val response = send(request)

    Right(unpack[Weather](response))
  }

}

trait Supplier {
  this: Configuration =>

  implicit def executor: ExecutionContextExecutor

  lazy val supplierConnector: SupplierConnector = new ZeroMQSupplierConnector(config.getInt("zeromq.ioThreads"), config.getString("zeromq.endpoint"))

}