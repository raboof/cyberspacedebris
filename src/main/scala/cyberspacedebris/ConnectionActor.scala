package cyberspacedebris

import java.net._

import scala.language.postfixOps
import scala.concurrent.duration._
import scala.util.Random

import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString

object ConnectionActor {
  def props(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef): Props =
    Props(new ConnectionActor(remote, local, connection))
  def name(remote: InetSocketAddress, local: InetSocketAddress) =
    s"$remote-${local.getPort}-${Random.nextInt()}".replaceAll("/", "")

  val prefixes = Map(
    Vector("PASV", "PORT") -> "ftp",
    Vector("GET", "POST", "HEAD", "OPTIONS") -> "http"
  ).flatMap { case (keys, value) => keys.map((_, value)) }

  private val TcpService = "^(\\w+)\\s*(\\d+)/tcp.*".r
  val servicesByPort = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/services"))
    .getLines
    .collect { case TcpService(name, port) => port.toInt -> name }
    .toMap

  def response(port: Int, protocol: Option[String]): (Option[String], Option[String]) = (port, protocol) match {
    case (23, _) => (Some("Debris Server\ndebris login: "), None)
    case _ => (None, Some("EHLO debris\n"))
  }
}

class ConnectionActor(remote: InetSocketAddress, local: InetSocketAddress, connection: ActorRef) extends Actor
    with ActorLogging {
  import ConnectionActor._

  log.debug(s"Got connection on port ${local.getPort}")
  context.setReceiveTimeout(5 seconds)

  def receive: Receive = initial
  def initial: Receive = data orElse close orElse sendOnTimeout()
  def received(proto: Option[String]): Receive = data orElse close orElse sendOnTimeout(proto)
  def sent: Receive = data orElse close orElse closeOnTimeout

  val data: Receive = {
    case Received(data) =>
      val readableData = data.decodeString("UTF-8").trim.replaceAll("[\r\n]+", "\\\\n")
      log.debug(s"Got data: $readableData")
      val proto = prefixes
        .find { case (prefix, protocol) => data.startsWith(prefix) }
        .map { case (_, protocol) => protocol }
      response(local.getPort, proto)._1.foreach(
        (fast: String) => connection ! Write(ByteString(fast))
      )
      context.become(received(proto))
  }

  val close: Receive = {
    case PeerClosed =>
      log.debug("Connection closed")
      report(None)
      context.stop(self)
  }

  def sendOnTimeout(proto: Option[String] = None): Receive = {
    case ReceiveTimeout =>
      log.debug("Connection timed out, trying to send something")
      response(local.getPort, proto)._2.foreach(
        (slow: String) => connection ! Write(ByteString(slow))
      )
      context.become(sent)
  }

  val closeOnTimeout: Receive = {
    case ReceiveTimeout =>
      log.debug("Connection timed out again, closing")
      report(None)
      context.stop(self)
  }

  def report(proto: Option[String]) = {
    val protocol = proto.orElse { servicesByPort.get(local.getPort) }
    context.system.eventStream.publish(Event(protocol, remote, local))
  }
}
