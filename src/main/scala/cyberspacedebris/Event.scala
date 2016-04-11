package cyberspacedebris

import java.net.InetSocketAddress

case class Event(protocol: Option[String], remote: InetSocketAddress, local: InetSocketAddress)
