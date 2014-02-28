package cscala
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._
import java.net.ConnectException

/**
 * Listen
 * Allow services to register (either by listening over a particular port,
 * or through internal JVM communication).
 * Allow processes to lookup services.
 */
object NS {

  private var impl: NameServer = null

  def isRunning(): Boolean = this.impl!=null
  
  /**
   * Returns a NameServer.  Finds one on a local JVM, finds one on this JVM or starts a new one.
   */
  def apply(): NameServer = {
    if (!isRunning()) {
      try {
          System.out.println("NS() trying to connect to local JVM")
          val nameServer = NetIO.clientConnection[Msg, Msg]("localhost", 7700, false)
          // wrap the foreign JVM
          impl = new ForeignNSWrapper(nameServer)
          return impl
      } catch {
        // no nameserver running on a local JVM
        case ce : java.net.ConnectException => {
          println("NS() starting a new local NameServer")
          // start a new one!
          impl = new LocalNS()
          return impl
        }
      } 
    } else {
      // local nameserver is already running
      println("NS() Already running locally")
      impl
    }
  }

  //  def main(args: Array[String]) = {
  //    val ns = NS();
  //  }

  //    // dummy insertion
  //    (proc {
  //      val respCh = OneOne[Boolean];
  //      // TODO: PORT!
  //      putCh ! (("DummyService", InetAddress.getByName("localhost"), respCh))
  //      println("Entered DummyService: " + (respCh?))
  //    })();
}