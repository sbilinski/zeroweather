package zeroweather.proxy

import java.util.Base64

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import org.velvia.msgpack._
import org.zeromq.ZMQ
import zeroweather.message.{ Weather, WeatherRequested }

import scala.util.{ Failure, Success }

class ZeroMQSupplierConnectorSpec extends TestKit(ActorSystem("ZeroMQSupplierConnectorSpec")) with WordSpecLike with Matchers with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  implicit val patience = PatienceConfig(Span(3, Seconds))
  implicit val executor = system.dispatcher

  lazy val fakeServerContext = ZMQ.context(1)

  def fakeServer(endpoint: String) = {
    val socket = fakeServerContext.socket(ZMQ.ROUTER)
    socket.bind(endpoint)
    socket
  }

  def serverConnector(endpoints: String*) = new ZeroMQSupplierConnector(system, endpoints)

  sealed trait Fixtures {
    val timestamp = 12345
    val countryCode = "pl"
    val city = "Warsaw"
    val temperatureInCelsius = BigDecimal("22.5")

    val weatherRequested = WeatherRequested(countryCode, city)
    val weather = Weather(timestamp, countryCode, city, temperatureInCelsius)
  }

  sealed implicit class ServerBehaviour(server: ZMQ.Socket) {
    def processRequestAndRespond(assumedRequest: WeatherRequested, assumedResponse: Weather) = {
      val identity = server.recv()
      val payload = server.recv()
      val message = unpack[WeatherRequested](Base64.getDecoder.decode(payload))

      message should be(assumedRequest)

      val reply = Base64.getEncoder.encode(pack(assumedResponse))
      server.send(identity, ZMQ.SNDMORE)
      server.send(reply)
    }
  }

  override def afterAll() = {
    system.terminate()
  }

  "ZeroMQSupplierConnector" should {
    "fetch weather using a request-reply pattern" in new Fixtures {
      Given("a single endpoint")
      val endpoint = "tcp://localhost:2000"

      And("a fake server")
      val server = fakeServer(endpoint)

      And("a connector")
      val connector = serverConnector(endpoint)

      When("a weather object is requested")
      val futureWeather = connector.fetchWeather(countryCode, city)

      And("the server replies")
      server.processRequestAndRespond(weatherRequested, weather)

      Then("the result should be equal to the expected value")
      whenReady(futureWeather) { result =>
        result should equal(Right(weather))
      }
    }

    "fail over to a second endpoint if the first one is unavailable" in new Fixtures {
      Given("a list of server endpoints")
      val unhealthyEndpoint = "tcp://localhost:3000"
      val healthyEndpoint = "tcp://localhost:3001"

      And("one healthy server instance")
      val server = fakeServer(healthyEndpoint)

      And("a connector which is querying the healthy server last")
      val connector = serverConnector(unhealthyEndpoint, healthyEndpoint)

      When("a weather object is requested")
      val futureWeather = connector.fetchWeather(countryCode, city)

      And("the server replies")
      server.processRequestAndRespond(weatherRequested, weather)

      Then("the result should be equal to the expected value")
      whenReady(futureWeather) { result =>
        result should equal(Right(weather))
      }
    }

    "fail when no endpoints are available" in new Fixtures {
      Given("a list of dead endpoints")
      val endpoints = List("tcp://localhost:4000", "tcp://localhost:4001")

      And("a connector")
      val connector = serverConnector(endpoints: _*)

      When("a weather object is requested")
      val futureWeather = connector.fetchWeather(countryCode, city)

      Then("the result should be equal to the expected value")

      whenReady(futureWeather) { result =>
        result should equal(Left(s"All ${endpoints.size} endpoints seem to be down."))
      }
    }

    "must throw an IllegalArgumentException when the endpoint list is empty" in {
      intercept[IllegalArgumentException] {
        serverConnector()
      }
    }
  }
}
