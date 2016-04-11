package cyberspacedebris

import java.net._

import scala.concurrent.duration._
import scala.concurrent._
import scala.language.postfixOps
import scala.util.Random

import akka.actor._
import akka.io._
import akka.pattern._
import akka.util._

import Tcp._

import com.sanoma.cda.geoip.MaxMindIpGeo

object MainActor {
  case class BindFailed(t: Throwable)
}

class MainActor extends Actor
    with ActorLogging {
  import MainActor._

  import context.system
  import context.dispatcher
  implicit val timeout: Timeout = 15 seconds

  val geoIp = new MaxMindIpGeo(getClass.getResourceAsStream("/maxmind/GeoLite2-City.mmdb"), synchronized = true)

  Await.result(context.actorOf(HttpApiActor.props(geoIp)) ? HttpApiActor.Ping, 5 seconds)

  val manager = IO(Tcp)
  val futures = Range(1, 65000)
    .map(port => manager ? Bind(self, new InetSocketAddress("0.0.0.0", port)))
    .map(_.recover { case t => BindFailed(t) })
  Future.sequence(futures)
    .pipeTo(self)
  log.info("Binding...")

  context.actorOf(EventLoggingActor.props(geoIp))

  def receive: Receive = {
    case x: Seq[Any] =>
      log.debug(s"Bound to ${x.filter(_.isInstanceOf[Bound]).length} ports")
    case Connected(remote, local) =>
      sender() ! Register(context.actorOf(ConnectionActor.props(remote, local, sender()), ConnectionActor.name(remote, local)))
    case e => println(e)
  }
}
