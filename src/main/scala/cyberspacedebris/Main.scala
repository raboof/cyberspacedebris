package cyberspacedebris

object Main {
  def main(args: Array[String]): Unit = {
    akka.Main.main(Array(classOf[MainActor].getName))
  }
}
