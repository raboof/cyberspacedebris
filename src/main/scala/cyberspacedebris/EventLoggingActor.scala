package cyberspacedebris

import akka.actor._

import com.sanoma.cda.geoip.MaxMindIpGeo

object EventLoggingActor {
  def props(geoIp: MaxMindIpGeo) = Props(new EventLoggingActor(geoIp))
}
class EventLoggingActor(geoIp: MaxMindIpGeo) extends Actor
    with ActorLogging {
  context.system.eventStream.subscribe(self, classOf[Event])

  override def receive = {
    case Event(protocol, remote, local) =>
      val location = geoIp.getLocation(remote.getAddress.getHostAddress)
      val loc = location.map(" from " + _).getOrElse("")
      log.info(s"${protocol.map(_ + " ").getOrElse("")}connection$loc (${remote.getAddress.getHostAddress}) on port ${local.getPort}")
  }
}
