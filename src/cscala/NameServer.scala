package cscala

import java.net.InetAddress

trait NameServer {
  val port = 7700;

  def register(name: String, address: InetAddress, port: Int): Boolean
  def lookup(name: String): Option[(InetAddress, Int)]
  // TODO deregister?

}

trait Msg {}

case class Register(name: String, address: InetAddress, port: Int) extends Msg
case class Lookup(name: String) extends Msg

case class Success(name: String, address: InetAddress, port: Int) extends Msg
case class Failure(name: String) extends Msg