package cscala

import java.net.InetAddress
import ox.cso.NetIO.Server
import ox.cso.NetIO._
import ox.cso._


trait NameServer {
  /**
   * For registering a service that is network accessible. Use NameServer.DEFAULT_TTL if necessary
   */
  def registerForeign(name: String, address: InetAddress, port: Int, ttl: Long): Boolean
  
  /**
   * For registering services that are only locally accessible
   */
  def registerLocal[Req, Resp](name: String, handle: (OutPort[Resp] with InPort[Req]) => Unit):Boolean

  /**
   * Lookup a network accessible service.  Returns the address and port.  Calling code is responsible 
   * for making the connection & handling errors if necessary.
   */
  def lookupForeign(name: String): Option[(InetAddress, Int)]

  def lookupAndConnect[Req <: Serial, Resp <: Serial](name: String): Option[OutPort[Req] with InPort[Resp]] = {
    return lookupForeign(name) match {
      case Some((addr, port)) => Some(NetIO.clientConnection[Req, Resp](addr, port, false)) // synchronous = false
      case None => None
    }
  }

  // TODO deregister?
}

object NameServer {
  val port = 7700
  val DEFAULT_TTL:Long = 1000*60*10 // 10 minutes
}

trait Msg {}

case class Register(name: String, address: InetAddress, port: Int, ttl: Long) extends Msg
case class Lookup(name: String) extends Msg

case class Success(name: String, address: InetAddress, port: Int) extends Msg
case class Failure(name: String) extends Msg