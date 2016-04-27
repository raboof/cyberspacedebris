package cyberspacedebris

import scala.concurrent.duration._

import akka.actor._
import akka.pattern._
import akka.util._
import akka.stream._
import akka.stream.scaladsl._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._

import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.marshallers.sprayjson._
import spray.json._

import com.sanoma.cda.geoip.MaxMindIpGeo

object HttpApiActor {
  def props(storage: ActorRef) = Props(new HttpApiActor(storage))

  case object Ping
  case object Pong
}

class HttpApiActor(val storage: ActorRef) extends Actor
    with Routes
    with ActorLogging {
  import HttpApiActor._

  implicit val executor = context.system.dispatcher
  implicit val materializer = ActorMaterializer()

  Http(context.system).bindAndHandle(routes, "0.0.0.0", port = 5431)
    .foreach(binding => log.info("Bound to " + binding))

  override def receive: Receive = {
    case Ping => sender() ! Pong
  }
}

trait Routes extends StorageProvider
    with SprayJsonSupport
    with DefaultJsonProtocol {
  implicit val locationFormat = jsonFormat3(Storage.Location.apply)
  implicit val eventFormat = jsonFormat3(Storage.StoredEvent.apply)
  implicit val timeout: Timeout = 5 seconds

  val routes = path("events") {
    get {
      onSuccess(storage ? Storage.Get) { events =>
        complete(StatusCodes.OK, events.asInstanceOf[Seq[Storage.StoredEvent]])
      }
    }
  } ~ pathPrefix("static") {
    getFromResourceDirectory("static")
  }
}
