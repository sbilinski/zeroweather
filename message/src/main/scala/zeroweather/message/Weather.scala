package zeroweather.message

case class Weather(timestamp: Long, countryCode: String, city: String, temperatureInCelsius: BigDecimal)

object Weather {
  //TODO: Replace with case class codecs once the new release of msgpack4s is published
  import java.io.{ DataInputStream, DataOutputStream }
  import org.velvia.msgpack._
  import org.velvia.msgpack.SimpleCodecs._
  import org.velvia.msgpack.RawStringCodecs._

  implicit object WeatherMsgPackCodec extends Codec[zeroweather.message.Weather] {
    def pack(out: DataOutputStream, item: Weather) = {
      out.write(0x01 | Format.MP_FIXARRAY)
      LongCodec.pack(out, item.timestamp)
      StringCodec.pack(out, item.countryCode)
      StringCodec.pack(out, item.city)
      StringCodec.pack(out, item.temperatureInCelsius.toString)
    }

    val unpackFuncMap: FastByteMap[WeatherMsgPackCodec.UnpackFunc] = FastByteMap[UnpackFunc](
      (0x01 | Format.MP_FIXARRAY).toByte -> { in: DataInputStream =>
        val timestamp = LongCodec.unpack(in)
        val countryCode = StringCodec.unpack(in)
        val city = StringCodec.unpack(in)
        val celsius = BigDecimal(StringCodec.unpack(in))

        Weather(timestamp, countryCode, city, celsius)
      }
    )
  }
}
