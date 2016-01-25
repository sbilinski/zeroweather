package zeroweather.proxy

import zeroweather.message.Weather

import scala.concurrent.Future
import scala.util.Random

trait SupplierConnector {
  def fetchWeather(countryCode: String, city: String): Future[Either[String, Weather]] = {
    //TODO: Replace with actual weather fetching
    countryCode match {
      case "ru" => Future.successful(Left("It's always cold in Russia!"))
      case "de" => throw new java.io.IOException("Failed to connect to the German server.")
      case _ => {
        Future.successful(
          Right(Weather(
            java.time.LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC),
            countryCode,
            city,
            23.0 + Random.nextDouble()
          ))
        )
      }
    }
  }
}
