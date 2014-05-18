package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO._
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
}