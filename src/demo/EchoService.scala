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

  val port = 3302

  private def startEchoService() {
    NetIO.serverPort(port, 0, false, handleClient).fork
  }

  private def handleClient(client: NetIO.Client[String, String]) = {
    proc("Handling EchoService request") {
      val incoming = (client?)
      client !(incoming)
    }.fork
  }

  def main(args: Array[String]): Unit = {
    // start listening over the internet
    startEchoService()

    // register with nameserver
    NS().registerForeign("EchoService", InetAddress.getByName("localhost"), this.port, NameServer.DEFAULT_TTL)

    // pretend to be a client.
    NS().lookupForeign("EchoService") match {
      case Some((addr, p)) => {
        // foreign server
        val server = NetIO.clientConnection[String, String](addr, p, false)
        println("sending   'Hello'")
        server ! "Hello"
        println("receiving '"+(server?)+"'")
      }
      case None => println("This shouldn't have happened.")
    }

    // must send Ctrl-C interrupt to stop this.
  }
}