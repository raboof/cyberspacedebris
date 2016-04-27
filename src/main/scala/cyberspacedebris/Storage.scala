package cyberspacedebris

import akka.actor._
import akka.persistence._

import com.sanoma.cda.geoip.MaxMindIpGeo

object Storage {
  case object Get
  case class Location(name: String, lat: Double, long: Double)
  case class StoredEvent(protocol: String, time: Long, location: Location)
}
class Storage(geoIp: MaxMindIpGeo) extends PersistentActor {
  import Storage._

  context.system.eventStream.subscribe(self, classOf[Event])

  override val persistenceId = "storage"

  var events: Seq[StoredEvent] = Seq()

  def receiveCommand = {
    case Get => sender() ! events
    case Event(protocol, remote, local) =>
      val location = geoIp.getLocation(remote.getAddress.getHostAddress)
      val name = location.map(loc => loc.city.map(_ + ", ").getOrElse("") + loc.region.map(_ + ", ").getOrElse("") + loc.countryName.getOrElse("")).getOrElse("")
      val l = location
        .flatMap(_.geoPoint)
        .map(point => Location(name, point.latitude, point.longitude))
        .getOrElse(Location(name, 40.4274, -111.9341))
      persist(StoredEvent(protocol.getOrElse(s"port ${remote.getPort}"), System.currentTimeMillis, l)) { event =>
        updateState(event)
      }
  }

  def receiveRecover = {
    case e: StoredEvent => updateState(e)
  }

  def updateState(event: StoredEvent) = {
    println(s"Adding $event")
    events = event +: events.filter(_.time > (System.currentTimeMillis - 200000))
  }
}

trait StorageProvider {
  val storage: ActorRef
}
