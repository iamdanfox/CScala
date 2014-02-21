package test
import java.net._
import test.NameServer.Name

/**
 * A type for interacting with a NameServer (a network accessible mapping from names to procs)
 */
trait NameServerMsg {}


case class Register(name: Name, address: InetAddress, port: Int) extends NameServerMsg
case class Lookup(name: Name) extends NameServerMsg

case class Success(name: Name, address: InetAddress, port: Int) extends NameServerMsg
case class Failure(name: Name) extends NameServerMsg