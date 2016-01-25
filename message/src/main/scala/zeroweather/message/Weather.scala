package zeroweather.message

case class Weather(timestamp: Long, countryCode: String, city: String, temperatureInCelsius: BigDecimal)
