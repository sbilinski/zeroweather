package zeroweather.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.{ Materializer, ActorMaterializer }
import spray.json.DefaultJsonProtocol
import zeroweather.message.Weather

import scala.concurrent.{ ExecutionContextExecutor, Future }

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
              case Right(temperature) => temperature
              case Left(error) => BadRequest -> error
            }
          }
        }
      }
    }
  }
}

object Proxy extends App with Service {
  override implicit val system = ActorSystem("Proxy")
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val supplierConnector = new SupplierConnector {}

  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
