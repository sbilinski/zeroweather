package zeroweather.proxy

import org.scalatest.{ BeforeAndAfterAll, Matchers, GivenWhenThen, WordSpec }
import org.velvia.msgpack._
import org.zeromq.ZMQ
import zeroweather.message.{ Weather, WeatherRequested }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Failure, Success }

class ZeroMQSupplierConnectorSpec extends WordSpec with Matchers with GivenWhenThen with BeforeAndAfterAll {

  lazy val endpoint = "tcp://localhost:12345"
  lazy val connector = new ZeroMQSupplierConnector(1, endpoint)
  lazy val fakeServer = {
    val socket = connector.context.socket(ZMQ.REP)
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
    fakeServer.close()
    connector.context.close()
  }

  "ZeroMQSupplierConnector" should {
    "fetch weather using a request-reply pattern" in new Fixtures {
      Given("a fake server")
      val server = fakeServer

      When("a weather object is requested")
      val futureWeather = connector.fetchWeather(countryCode, city)

      And("the server replies")
      val msg = server.recv()
      unpack[WeatherRequested](msg) should be(weatherRequested)
      server.send(pack(weather))

      Then("the result should be equal to the expected value")
      futureWeather onComplete {
        case Success(value) => value should equal(Right(weather))
        case Failure(error) => fail(error)
      }
    }
  }

}
