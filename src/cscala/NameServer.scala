package cscala

import java.net.InetAddress

trait NameServer {
  /**
   * For registering a service that is network accessible.
   */
  def registerForeign(name: String, address: InetAddress, port: Int): Boolean

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
}

trait Msg {}

case class Register(name: String, address: InetAddress, port: Int) extends Msg
case class Lookup(name: String) extends Msg

case class Success(name: String, address: InetAddress, port: Int) extends Msg
case class Failure(name: String) extends Msg