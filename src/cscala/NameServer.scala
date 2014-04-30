package cscala

import java.net.InetAddress
import ox.cso.NetIO.Server
import ox.cso.NetIO._
import ox.cso.OutPort
import ox.cso.InPort
import ox.cso.NetIO


trait NameServer {
  
  /**
   * The IP address at which this particular NameServer can be reached.
   */
  def nameServerAddress : InetAddress = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()) // TODO: final?
  
  /**
   * For registering a service that is network accessible. Use NameServer.DEFAULT_TTL if necessary.
   * Returns true if the Record has been successfully saved, false otherwise
   */
  def registerForeign(name: String, address: InetAddress, port: NameServer.Port, ttl: NameServer.TTL): Boolean

  /**
   * Register a service and bind it to a socket.
   */
  def registerAndBind[Req, Rep](name: String, port: NameServer.Port, ttl: NameServer.TTL, handleClient: Client[Req, Rep] => Unit): Boolean = synchronized {
    // `synchronized` keyword makes it atomic
    // first, check if the name is in use.
    lookupForeign(name) match {
      case Some(_) => return false // name already in use 
      case None =>
        try {
          // bind handler to a port
          NetIO.serverPort(port, 0, false, handleClient).fork
          // insert Record into Registry
          registerForeign(name, nameServerAddress, port, ttl)
          return true
        } catch {
          case e: java.net.BindException => return false // Port already in use
          case _ => return false
        }
    }
  }
    

  /**
   * Lookup a network accessible service.  Returns the address and port.  Calling code is responsible 
   * for making the connection & handling errors if necessary.
   */
  def lookupForeign(name: String): Option[(InetAddress, NameServer.Port)]

  /**
   * Lookup a service. Returns the address and port or throws a NameNotFoundException.
   */
  def lookupForeign2(name: String): (InetAddress, NameServer.Port) = // TODO better name
    lookupForeign(name) match {
      case Some(v) => return v
      case None => throw new NameServer.NameNotFoundException(name) // beware, scala treats all exceptions as RuntimeExceptions
    }
  
  def lookupAndConnect[Req, Resp](name: String): Option[OutPort[Req] with InPort[Resp]] = {
    return lookupForeign(name) match {
      case Some((addr, port)) => Some(NetIO.clientConnection[Req, Resp](addr, port, false)) // synchronous = false
      case None => None
    }
  }

  /**
   * Connect to a service. Returns the address and port or throws a NameNotFoundException.
   */
  def lookupAndConnect2[Req, Resp](name: String): OutPort[Req] with InPort[Resp] = // TODO better name
    lookupAndConnect(name) match {
      case Some(conn) => return conn
      case None => throw new NameServer.NameNotFoundException(name)
    }

  // TODO deregister?
}

object NameServer {
  type Port = Int
  type TTL = Long
  
  val NAMESERVER_PORT:Port = 7700
  val DEFAULT_TTL:TTL = 1000*60*10 // 10 minutes
  
  class NameNotFoundException(name: String) extends Exception {} 
}
