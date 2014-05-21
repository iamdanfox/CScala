package demo

import ox.CSO._
import ox.cso.NetIO
import java.net.InetAddress

object CloseTest {
  def main(args: Array[String]): Unit = {

    NetIO.serverPort(7843, 0, false, handleClient).fork

    val server = NetIO.clientConnection[String, String](InetAddress.getLocalHost(), 7843, false)
    server ! "Hello, world!"
    println("Response: " + (server?))
    server ! "Message 2" // java.net.SocketException: Broken pipe

  }

  private def handleClient(client: NetIO.Client[String, String]) = {
    proc("CloseTest") {
      val incoming = (client?)
      client ! incoming
      client.close
      println("sent close")
      Thread.sleep(4000)
    }.fork
  }
}