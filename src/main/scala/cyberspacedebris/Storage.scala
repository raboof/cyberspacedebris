package cyberspacedebris

import java.net.InetSocketAddress

import akka.actor._
import akka.persistence._

import com.sanoma.cda.geoip.MaxMindIpGeo

object Storage {
  case object Get
  case class Location(name: String, lat: Double, long: Double)
  case class StoredEvent(remote: InetSocketAddress, local: InetSocketAddress, protocol: String, time: Long, location: Location, data: Option[Array[Byte]])
}
class Storage(geoIp: MaxMindIpGeo) extends PersistentActor {
  import Storage._

  context.system.eventStream.subscribe(self, classOf[Event])

  override val persistenceId = "storage"

  var events: Seq[StoredEvent] = Seq()

  def receiveCommand = {
    case Get => sender() ! events
    case Event(protocol, remote, local, data) =>
      val location = geoIp.getLocation(remote.getAddress.getHostAddress)
      val name = location.map(loc => loc.city.map(_ + ", ").getOrElse("") + loc.region.map(_ + ", ").getOrElse("") + loc.countryName.getOrElse("")).getOrElse("")
      val l = location
        .flatMap(_.geoPoint)
        .map(point => Location(name, point.latitude, point.longitude))
        .getOrElse(Location(name, 40.4274, -111.9341))
      persist(StoredEvent(remote, local, protocol.getOrElse(s"port ${remote.getPort}"), System.currentTimeMillis, l, data)) { event =>
        updateState(event)
      }
  }

  def receiveRecover = {
    case e: StoredEvent => updateState(e)
  }

  def updateState(event: StoredEvent) = {
    println(s"Adding $event")
    events = event +: events.filter(_.time > (System.currentTimeMillis - 200 * 1000000))
  }
}

trait StorageProvider {
  val storage: ActorRef
}
