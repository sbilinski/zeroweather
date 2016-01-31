package zeroweather.message

case class WeatherRequested(countryCode: String, city: String)

object WeatherRequested {
  //TODO: Replace with case class codecs once the new release of msgpack4s is published
  import java.io.{ DataInputStream, DataOutputStream }

  import org.velvia.msgpack.RawStringCodecs._
  import org.velvia.msgpack._

  implicit object WeatherRequestedMsgPackCodec extends Codec[WeatherRequested] {
    def pack(out: DataOutputStream, item: WeatherRequested) = {
      out.write(0x01 | Format.MP_FIXARRAY)
      StringCodec.pack(out, item.countryCode)
      StringCodec.pack(out, item.city)
    }

    val unpackFuncMap: FastByteMap[WeatherRequestedMsgPackCodec.UnpackFunc] = FastByteMap[UnpackFunc](
      (0x01 | Format.MP_FIXARRAY).toByte -> { in: DataInputStream =>
        val countryCode = StringCodec.unpack(in)
        val city = StringCodec.unpack(in)

        WeatherRequested(countryCode, city)
      }
    )
  }
}