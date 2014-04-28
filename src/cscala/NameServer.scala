package cscala

import java.net.InetAddress
import ox.cso.NetIO.Server
import ox.cso.NetIO._
import ox.cso.OutPort
import ox.cso.InPort
import ox.cso.NetIO


trait NameServer {
  /**
   * For registering a service that is network accessible. Use NameServer.DEFAULT_TTL if necessary
   */
  def registerForeign(name: String, address: InetAddress, port: NameServer.Port, ttl: NameServer.TTL): Boolean
  
  /**
   * For registering services that are only locally accessible
   */
  //  def registerLocal[Req <: Serial, Rep <: Serial](name: String, handle: Client[Req, Rep] => Unit):Boolean

  /**
   * Lookup a network accessible service.  Returns the address and port.  Calling code is responsible 
   * for making the connection & handling errors if necessary.
   */
  def lookupForeign(name: String): Option[(InetAddress, NameServer.Port)]

  def lookupAndConnect[Req, Resp](name: String): Option[OutPort[Req] with InPort[Resp]] = {
    return lookupForeign(name) match {
      case Some((addr, port)) => Some(NetIO.clientConnection[Req, Resp](addr, port, false)) // synchronous = false
      case None => None
    }
  }

  // TODO deregister?
}

object NameServer {
  type Port = Int
  type TTL = Long
  
  val NAMESERVER_PORT:Port = 7700
  val DEFAULT_TTL:TTL = 1000*60*10 // 10 minutes
}
