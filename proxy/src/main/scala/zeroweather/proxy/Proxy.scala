package zeroweather.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.{ ActorMaterializer, Materializer }
import spray.json._
import zeroweather.message.Weather

import scala.concurrent.ExecutionContextExecutor

trait Protocols extends DefaultJsonProtocol {
  implicit val weatherFormat = jsonFormat4(Weather.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  val supplierConnector: SupplierConnector

  implicit def exceptionHandler = ExceptionHandler {
    case e: java.io.IOException =>
      extractUri { uri =>
        complete(HttpResponse(ServiceUnavailable, entity = e.getMessage))
      }
  }

  val routes = {
    pathPrefix("weather" / """[a-z]{2}""".r / """[a-zA-Z ]+""".r) { (countryCode, city) =>
      pathEnd {
        get {
          complete {
            supplierConnector.fetchWeather(countryCode, city).map[ToResponseMarshallable] {
              case Right(weather) => weather
              case Left(error) => BadRequest -> error
            }
          }
        }
      }
    }
  }
}

object Proxy extends App with Service with Supplier with Configuration {
  override implicit val system = ActorSystem("Proxy")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
