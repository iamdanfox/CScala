package cscala

import ox.CSO._
import ox.cso.Connection._

class FullyLocalNS {
  
  private val registry = new collection.mutable.HashMap[String,(Client[Any,Any] => Unit, Registry.Timestamp, NameServer.TTL)]()

  def register[Req, Resp](name: String, ttl: NameServer.TTL, handleClient: (Client[Req, Resp]) => Unit): Boolean = {
    registry.get(name) match {
      case Some((handlefn, timestamp, ttl)) if timestamp+ttl > System.currentTimeMillis() => {
        
      }
      case None => {
         return false
      }
    }
  }  
  
  /**
   * Lookup a name in the nameserver and 'connect' to the service. Throw an exception otherwise.
   */
  def lookup[Req,Resp](name: String) : Server[Req,Resp] = synchronized {
    lookup2[Req,Resp](name) match {
      case Some(server) => return server
      case None => throw new NameServer.NameNotFoundException(name)
    }
  }
  
  /**
   * Tentative lookup and connect
   */
  def lookup2[Req,Resp](name: String) : Option[Server[Req,Resp]] = synchronized {
    registry.get(name) match {
      case Some(handlefn) => {
        val handleFunction = handlefn.asInstanceOf[Client[Req,Resp] => Unit]
        // 'connect' to the server
        val conn = ox.cso.Connection.OneOne[Req,Resp]
        proc { handleFunction(conn.client) }.fork 
        return Some(conn.server);
      }
      case None => return None
    }
  }
  
}