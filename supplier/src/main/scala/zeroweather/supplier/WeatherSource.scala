package zeroweather.supplier

import zeroweather.message.{ Weather, WeatherRequested }

trait WeatherSourceConnector {
  def fetchWeather(request: WeatherRequested): Weather
}

class FakeWeatherSourceConnector extends WeatherSourceConnector {
  override def fetchWeather(request: WeatherRequested): Weather = {
    Weather(
      java.time.LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC),
      request.countryCode,
      request.city,
      23.0 + scala.util.Random.nextDouble()
    )
  }
}

trait WeatherSource {
  lazy val weatherSourceConnector: WeatherSourceConnector = new FakeWeatherSourceConnector
}
