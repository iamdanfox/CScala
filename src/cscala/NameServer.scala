package cscala

import java.net.InetAddress

import ox.cso.NetIO
import ox.cso.Connection.Server
import NameServer.Port
import NameServer.TTL


trait NameServer {
  
  /**
   * The IP address at which this particular NameServer can be reached.
   */
  def nameServerAddress : InetAddress = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()) // TODO: final?
  
//  def register(name:String, port:Port, ttl:TTL = NameServer.DEFAULT_TTL) : Boolean = 
//    return registerForeign(name, this.nameServerAddress, port, ttl)
  
  /**
   * For registering an arbritary service that is network accessible. Use NameServer.DEFAULT_TTL if necessary.
   * Returns true if the Record has been successfully saved, false otherwise
   */
  def registerAddr(name: String, address: InetAddress, port: Port, ttl: TTL = NameServer.DEFAULT_TTL): Boolean

  /**
   * Lookup a network accessible service.  Returns the address and port.  Calling code is responsible 
   * for making the connection & handling errors if necessary.
   */
  def lookupAddr(name: String): Option[(InetAddress, NameServer.Port)]

  /**
   * Lookup a service. Returns the address and port or throws a NameNotFoundException.
   */
//  def lookupForeign2(name: String): (InetAddress, NameServer.Port) = // TODO better name
//    lookupForeign(name) match {
//      case Some(v) => return v
//      case None => throw new NameServer.NameNotFoundException(name) // beware, scala treats all exceptions as RuntimeExceptions
//    }
  
  def lookup[Req, Resp](name: String): Option[Server[Req,Resp]]

  /**
   * Connect to a service. Returns the address and port or throws a NameNotFoundException.
   */
  def lookupConf[Req, Resp](name: String): Server[Req,Resp] = // TODO better name
    lookup[Req,Resp](name) match {
      case Some(conn) => return conn
      case None => throw new NameServer.NameNotFoundException(name)
    }
}

object NameServer {
  type Port = Int
  type TTL = Long
  
  val NAMESERVER_PORT:Port = 7700
  val DEFAULT_TTL:TTL = 1000*60*10 // 10 minutes
  val TEN_MIN_TTL:TTL = 1000*60*10 // 10 minutes
  
  class NameNotFoundException(name: String) extends Exception {} 
}
