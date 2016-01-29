package zeroweather.proxy

import java.util.Base64

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest._
import org.velvia.msgpack._
import org.zeromq.ZMQ
import zeroweather.message.{ Weather, WeatherRequested }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

class ZeroMQSupplierConnectorSpec extends TestKit(ActorSystem("ZeroMQSupplierConnectorSpec")) with WordSpecLike with Matchers with GivenWhenThen with BeforeAndAfterAll {

  lazy val endpoint = "tcp://localhost:12345"
  lazy val connector = new ZeroMQSupplierConnector(system, endpoint)
  lazy val fakeServerContext = ZMQ.context(1)
  lazy val fakeServer = {
    val socket = fakeServerContext.socket(ZMQ.ROUTER)
    socket.bind(endpoint)
    socket
  }

  sealed trait Fixtures {
    val timestamp = 12345
    val countryCode = "pl"
    val city = "Warsaw"
    val temperatureInCelsius = BigDecimal("22.5")

    val weatherRequested = WeatherRequested(countryCode, city)
    val weather = Weather(timestamp, countryCode, city, temperatureInCelsius)
  }

  override def afterAll() = {
    system.terminate()
  }

  "ZeroMQSupplierConnector" should {
    "fetch weather using the dealer-router pattern" in new Fixtures {
      Given("a fake server")
      val server = fakeServer

      When("a weather object is requested")
      val futureWeather = connector.fetchWeather(countryCode, city)

      And("the server replies")
      val identity = server.recv()
      val payload = server.recv()
      val message = unpack[WeatherRequested](Base64.getDecoder.decode(payload))

      message should be(weatherRequested)

      val reply = Base64.getEncoder.encode(pack(weather))
      server.send(identity, ZMQ.SNDMORE)
      server.send(reply)

      Then("the result should be equal to the expected value")
      futureWeather onComplete {
        case Success(value) => value should equal(Right(weather))
        case Failure(error) => fail(error)
      }
    }
  }

}
