package cyberspacedebris

import akka.actor._

import com.sanoma.cda.geoip.MaxMindIpGeo

object Storage {
  case object Get
  case class Location(name: String, lat: Double, long: Double)
  case class HttpEvent(protocol: String, time: Long, location: Location)
}
class Storage(geoIp: MaxMindIpGeo) extends Actor {
  import Storage._

  var events: Seq[HttpEvent] = Seq()

  def receive = {
    case Get => sender() ! events
    case Event(protocol, remote, local) =>
      val location = geoIp.getLocation(remote.getAddress.getHostAddress)
      val name = location.map(loc => loc.city.map(_ + ", ").getOrElse("") + loc.region.map(_ + ", ").getOrElse("") + loc.countryName.getOrElse("")).getOrElse("")
      val l = location
        .flatMap(_.geoPoint)
        .map(point => Location(name, point.latitude, point.longitude))
        .getOrElse(Location(name, 40.4274, -111.9341))
      events = HttpEvent(protocol.getOrElse(s"port ${remote.getPort}"), System.currentTimeMillis, l) +:
        events.filter(_.time > (System.currentTimeMillis - 200000))
  }
}

trait StorageProvider {
  val storage: ActorRef
}
