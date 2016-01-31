package zeroweather.supplier

import java.util.{ Base64, UUID }

import akka.actor.{ ActorSystem, Props }
import akka.testkit.TestKit
import com.ibm.spark.communication.actors.RouterSocketActor
import com.typesafe.scalalogging.LazyLogging
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.velvia.msgpack._
import org.zeromq.ZMQ
import zeroweather.message.{ Weather, WeatherRequested }

class ServiceSpec extends TestKit(ActorSystem("ServiceSpec")) with WordSpecLike with Matchers with GivenWhenThen with MockFactory with BeforeAndAfterAll with LazyLogging {

  lazy val endpoint = "tcp://localhost:12345"
  lazy val weatherSourceConnector = mock[WeatherSourceConnector]

  lazy val router = system.actorOf(Props(classOf[RouterActor], weatherSourceConnector))
  lazy val routerSocket = system.actorOf(Props(classOf[RouterSocketActor], endpoint, router, None))

  lazy val fakeClientContext = ZMQ.context(1)
  lazy val fakeClient = {
    logger.debug(s"Connecting fake ZMQ client to ${endpoint}")
    val socket = fakeClientContext.socket(ZMQ.DEALER)
    socket.setIdentity(UUID.randomUUID().toString.getBytes(ZMQ.CHARSET))
    socket.connect(endpoint)
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

  "Service" should {

    "consume incoming request and respond with a weather object" in new Fixtures {
      Given("the server is initialized")
      //TODO: Ping actors and Await a reply
      routerSocket
      Thread.sleep(1000)
      logger.warn("Assuming the server is ready")

      And("a ZeroMQ client")
      val client = fakeClient

      And("a fixed weather source response")
      (weatherSourceConnector.fetchWeather _).expects(weatherRequested).returning(weather)

      When("the client sends a weather request")
      val request = Base64.getEncoder.encode(pack(weatherRequested))
      client.send(request)

      Then("the client should receive a weather response")
      val res = client.recv()
      val obj = unpack[Weather](Base64.getDecoder.decode(res))

      And("the response should be equal to the expected value")
      obj should be(weather)
    }

  }
}
