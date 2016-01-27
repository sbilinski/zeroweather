package zeroweather.message

import org.scalatest.{ Matchers, WordSpec }
import org.velvia.msgpack._

class WeatherSpec extends WordSpec with Matchers {

  sealed trait Fixtures {
    val weather = Weather(12345, "pl", "Warsaw", BigDecimal("25.5"))
  }

  "Weather" should {
    "serialize to 'msgpack' and deserialize back to the origin object" in new Fixtures {
      val msg = pack(weather)
      val out = unpack[Weather](msg)

      out should be(weather)
    }
  }

}
