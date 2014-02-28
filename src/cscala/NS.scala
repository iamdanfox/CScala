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
 * Returns a nameserver.
 *
 * Returns the locally running one if possible, otherwise, will find one running on this JVM.
 *
 * Starts a new local NameServer if no other options.
 */
object NS {

  private var impl: NameServer = null

  def isRunning(): Boolean = this.impl != null

  def apply(): NameServer = { // TODO is this threadsafe?
    if (isRunning()) {
      // local nameserver is already running
      println("NS() Already running locally")
      return impl
    } else findForeignNS() match {
      case Some(foreignNS) =>
        System.out.println("NS() trying to connect to local JVM")
        // wrap the foreign server object with `register`, `lookup` methods
        impl = new ForeignNSWrapper(foreignNS)
        return impl
      case None =>
        println("NS() starting a new local NameServer")
        // start a new one, serving properly
        impl = new ServeLocalNS()
        return impl
    }
  }

  private def findForeignNS(): Option[ox.cso.NetIO.Server[Msg, Msg]] = {
    try {
      return Some(NetIO.clientConnection[Msg, Msg]("localhost", 7700, false))
    } catch {
      // couldn't connect to localhost:7700, 
      case ce: java.net.ConnectException => return None
    }
  }
}