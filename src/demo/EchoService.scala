package demo

import ox.CSO.OneOne
import ox.CSO.proc
import ox.cso.NetIO

import cscala.NS
import java.net.InetAddress

/**
 * A trivial service that simply echoes back the message it receives then terminates the client.
 */
object EchoService {
  
  val port = 3302
  protected var thread = null.asInstanceOf[ox.cso.ThreadHandle]
  
  private def startEchoService(){
       this.thread = NetIO.serverPort(port, 0, false, handleClient).fork
  }
  
  private def stopEchoService(){
    this.thread.interrupt // this is messy
  }
  
  private def handleClient(client: NetIO.Client[String, String]) = {
      proc("Handling EchoService request"){
          val incoming = (client?)
          client!("Echo: "+incoming)
      }.fork
  }
  
  def main(args: Array[String]): Unit = {
    // start listening over the internet
    startEchoService()
    
    // register with nameserver
    NS().register("EchoService", InetAddress.getByName("localhost"), this.port)
    
    NS().lookup("EchoService") match {
      case Some((addr,port)) => {
        // TODO: send something, print out the response
      }
      case None => println("This shouldn't have happened.")
    }
  }
}