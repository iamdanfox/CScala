package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.cso.NetIO._
import ox.CSO._
import ox.cso._
import cscala.NameServer._
import cscala.Registry._


/**
 * Wrapper for a registry of (InetAddress, Port) pairs.
 */
class NetworkNS extends NameServer {
  
  protected val registry = new Registry[(InetAddress, Port)]()
  
  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    val rtnCh = OneOne[Boolean]
    val timestamp = System.currentTimeMillis()
    registry.put ! ((name, ((address, port), timestamp, ttl), rtnCh))
    return rtnCh?
  }

  /**
   * Looks up the name in the registry
   */
  override def lookupForeign(name: String): Option[(InetAddress, Port)] = {
    val rtnCh = OneOne[Option[registry.Record]]
    registry.get ! ((name, rtnCh))
    return (rtnCh?) match {
      case Some((payload, timestamp, ttl)) => Some(payload) // slightly less data returned
      case None => None
    }
  }
  
  
  /**
   * Register a service and bind it to a socket.
   */
  def registerAndBind[Req, Rep](name: String, port: Port, ttl: TTL = NameServer.DEFAULT_TTL, handleClient: Client[Req, Rep] => Unit): Boolean = synchronized {
    // `synchronized` keyword makes it atomic
    // first, check if the name is in use.
    lookupForeign(name) match {
      case Some(_) => return false // name already in use 
      case None =>
        try {
          // bind handler to a port
          ox.cso.NetIO.serverPort(port, 0, false, handleClient).fork
          // insert Record into Registry
          registerForeign(name, nameServerAddress, port, ttl)
          return true
        } catch {
          case e: java.net.BindException => return false // Port already in use
          case _ => return false
        }
    }
  }
}