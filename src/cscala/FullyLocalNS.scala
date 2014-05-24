package cscala

import ox.CSO._
import ox.cso.Connection._

class FullyLocalNS extends Connectable {
  
//  private val registry = new collection.mutable.HashMap[String,(Client[Any,Any] => Unit, Registry.Timestamp, NameServer.TTL)]()
  private val registry = new Registry[Client[Any,Any] => Unit]()

  def register[Req, Resp](name: String, ttl: NameServer.TTL, handleClient: (Client[Req, Resp]) => Unit): Boolean = {
    
    val ret = new OneOne[Boolean]
    registry.put!((name, (handleClient.asInstanceOf[Client[Any,Any] => Unit], System.currentTimeMillis(), ttl), ret))
    return (ret?)
  }  
  
  /**
   * Lookup a name in the nameserver and 'connect' to the service. Throw an exception otherwise.
   */
  def connect2[Req, Resp](name: String) : Server[Req,Resp] = synchronized {
    connect[Req,Resp](name) match {
      case Some(server) => return server
      case None => throw new NameServer.NameNotFoundException(name)
    }
  }
  
  /**
   * Tentative lookup and connect
   */
  def connect[Req, Resp](name: String) : Option[Server[Req,Resp]] = synchronized {
    val ret = new OneOne[Option[registry.Record]]
    registry.get!((name, ret))
    ret? match {
      case Some(recordTuple) => {
        val handleFunction = recordTuple._1.asInstanceOf[Client[Req,Resp] => Unit]
        // 'connect' to the server
        val conn = ox.cso.Connection.OneOne[Req,Resp]
        proc { handleFunction(conn.client) }.fork 
        return Some(conn.server);
      }
      case None => return None
    }
  }
  
  /**
   * (So far, this only seems useful for writing tests)
   */
  def terminate() = {
    registry.terminate!()
  }
}