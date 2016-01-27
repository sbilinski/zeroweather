package zeroweather.supplier

import org.scalamock.scalatest.MockFactory
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpec }
import org.velvia.msgpack._
import org.zeromq.ZMQ
import zeroweather.message.{ Weather, WeatherRequested }

class ServiceSpec extends WordSpec with Matchers with GivenWhenThen with MockFactory with BeforeAndAfterAll with Service {

  override val context = ZMQ.context(2)
  override val endpoint = "tcp://localhost:12345"
  override val weatherSourceConnector = mock[WeatherSourceConnector]

  lazy val fakeClient = {
    val socket = context.socket(ZMQ.REQ)
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
    fakeClient.close()
    socket.close()
    context.close()
  }

  "Service" should {

    "consume incoming request and respond with a weather object" in new Fixtures {
      Given("a ZeroMQ client")
      val client = fakeClient

      And("an initialized server binding")
      socket.getType

      And("a fixed weather source response")
      (weatherSourceConnector.fetchWeather _).expects(weatherRequested).returning(weather)

      When("the client sends a weather request")
      client.send(pack(weatherRequested))

      And("the message handler is invoked")
      handleMessage(false)

      Then("the client should receive a weather response")
      val res = client.recv()
      val obj = unpack[Weather](res)

      And("the response should be equal to the expected value")
      obj should be(weather)
    }

  }
}
