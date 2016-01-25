package zeroweather.proxy

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Matchers, WordSpec }
import zeroweather.message.Weather

import scala.concurrent.Future

class ServiceSpec extends WordSpec with Matchers with MockFactory with ScalatestRouteTest with Service {

  sealed trait Fixtures {
    val timestamp = 12345
    val countryCode = "pl"
    val city = "Warsaw"
    val temperatureInCelsius = BigDecimal("22.5")

    val weather = Weather(timestamp, countryCode, city, temperatureInCelsius)
  }

  override val supplierConnector = mock[SupplierConnector]

  "Service" should {

    "fetch weather for a given city and country" in new Fixtures {
      (supplierConnector.fetchWeather _).expects(countryCode, city).returning(Future.successful(Right(weather)))

      Get(s"/weather/${countryCode}/${city}") ~> routes ~> check {
        status shouldBe OK
        contentType shouldBe `application/json`
        responseAs[Weather] shouldBe weather
      }
    }

    "respond with BadRequest when supplier connector returns an error message" in new Fixtures {
      (supplierConnector.fetchWeather _).expects(countryCode, city).returning(Future.successful(Left("supplier msg")))

      Get(s"/weather/${countryCode}/${city}") ~> routes ~> check {
        status shouldBe BadRequest
        responseAs[String] shouldBe "supplier msg"
      }
    }

    "respond with ServiceUnavailable when supplier connector fails with an IOException" in new Fixtures {
      (supplierConnector.fetchWeather _).expects(countryCode, city).returning(Future.failed(new java.io.IOException("error msg")))

      Get(s"/weather/${countryCode}/${city}") ~> routes ~> check {
        status shouldBe ServiceUnavailable
        responseAs[String] shouldBe "error msg"
      }
    }

  }
}
