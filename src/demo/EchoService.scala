package demo

import ox.CSO.OneOne
import ox.CSO.proc
import ox.cso.NetIO

import cscala._
import java.net.InetAddress

/**
 * A trivial service that simply echoes back the message it receives then terminates the client.
 */
object EchoService {

  def port = 3302

  def startEchoService() {
    NetIO.serverPort(port, 0, false, handleClient).fork
  }

  private def handleClient(client: NetIO.Client[String, String]) = {
    proc("Handling EchoService request") {
//      print("EchoService handleClient..")
      val incoming = (client?)
      client ! incoming
//      println("done")
    }.fork
  }
}