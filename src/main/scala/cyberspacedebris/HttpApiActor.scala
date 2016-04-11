package cyberspacedebris

import akka.actor._
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
  def props(geoIp: MaxMindIpGeo) = Props(new HttpApiActor(geoIp))

  case object Ping
  case object Pong
  case class Location(name: String, lat: Double, long: Double)
  case class HttpEvent(protocol: String, time: Long, location: Location)
}
class HttpApiActor(geoIp: MaxMindIpGeo) extends Actor
    with ActorLogging
    with SprayJsonSupport
    with DefaultJsonProtocol {
  import HttpApiActor._

  context.system.eventStream.subscribe(self, classOf[Event])

  implicit val executor = context.system.dispatcher
  implicit val materializer = ActorMaterializer()
  var events: Seq[HttpEvent] = Seq()

  implicit val locationFormat = jsonFormat3(Location.apply)
  implicit val httpEventFormat = jsonFormat3(HttpEvent.apply)

  val routes = path("events") {
    get {
      complete(StatusCodes.OK, events)
    }
  } ~ pathPrefix("static") {
    getFromResourceDirectory("static")
  }

  Http(context.system).bindAndHandle(routes, "0.0.0.0", port = 5431)
    .foreach(binding => log.info("Bound to " + binding))

  override def receive: Receive = {
    case Ping => sender() ! Pong
    case Event(protocol, remote, local) =>
      val location = geoIp.getLocation(remote.getAddress.getHostAddress)
      val name = location.map(loc => loc.city.map(_ + ", ").getOrElse("") + loc.region.map(_ + ", ").getOrElse("") + loc.countryName.getOrElse("")).getOrElse("")
      val l = location
        .flatMap(_.geoPoint)
        .map(point => Location(name, point.latitude, point.longitude))
        .getOrElse(Location(name, 40.4274, -111.9341))
      events = HttpEvent(protocol.getOrElse(s"port ${remote.getPort}"), System.currentTimeMillis, l) +: events
  }
}
