package cscala

import ox.cso.NetIO

/**
 * Returns a nameserver.
 *
 * Returns the locally running one if possible, otherwise, will find one running on this JVM.
 *
 * Starts a new local NameServer if no other options.
 */
object NS {

  private var impl: NameServer = null

  def localRunning(): Boolean = this.impl != null

  def apply(): NameServer = { // TODO is this threadsafe?
    if (localRunning()) {
      // local nameserver is already running
      //println("NS() Already running locally")
      return impl
    } else findForeignNS() match {
      case Some(foreignNS) =>
        // wrap the foreign server object with `register`, `lookup` methods 
        return new ForeignNSWrapper(foreignNS) // new connection is made for every single request. TODO: improve
      case None =>
        //println("NS() starting a new local NameServer")
        // start a new one, serving properly
        impl = new ServeLocalNS()
        return impl
    }
  }

  private def findForeignNS(): Option[ox.cso.NetIO.Server[Msg, Msg]] = {
    try {
      return Some(NetIO.clientConnection[Msg, Msg]("localhost", NameServer.NAMESERVER_PORT, false))
    } catch {
      // couldn't connect to localhost:7700, 
      case ce: java.net.ConnectException => return None
    }
  }
}