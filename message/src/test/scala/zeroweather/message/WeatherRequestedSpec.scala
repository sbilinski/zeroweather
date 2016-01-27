package zeroweather.message

import org.scalatest.{ Matchers, WordSpec }
import org.velvia.msgpack._

class WeatherRequestedSpec extends WordSpec with Matchers {

  sealed trait Fixtures {
    val weatherRequested = WeatherRequested("pl", "Warsaw")
  }

  "WeatherRequested" should {
    "serialize to 'msgpack' and deserialize back to the origin object" in new Fixtures {
      val msg = pack(weatherRequested)
      val out = unpack[WeatherRequested](msg)

      out should be(weatherRequested)
    }
  }

}
