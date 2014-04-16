package cscala

import java.net.InetAddress

trait NameServer {
  /**
   * For registering a service that is network accessible. Use NameServer.DEFAULT_TTL if necessary
   */
  def registerForeign(name: String, address: InetAddress, port: Int, ttl: Long): Boolean
  
  /**
   * For registering services that are only locally accessible
   */
  //  def registerLocal[Req <: Serial, Rep <: Serial](name: String, handle: Client[Req, Rep] => Unit):Boolean

  /**
   * Lookup a network accessible service.  Returns the address and port.  Calling code is responsible 
   * for making the connection & handling errors if necessary.
   */
  def lookupForeign(name: String): Option[(InetAddress, Int)]

  // TODO deregister?
}

object NameServer {
  val port = 7700
  val DEFAULT_TTL = 60*10 // 10 minutes
}

trait Msg {}

case class Register(name: String, address: InetAddress, port: Int, ttl: Long) extends Msg
case class Lookup(name: String) extends Msg

case class Success(name: String, address: InetAddress, port: Int) extends Msg
case class Failure(name: String) extends Msg