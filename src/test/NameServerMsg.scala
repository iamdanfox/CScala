package test
import java.net._ 

/**
 * A type for interacting with a NameServer (a network accessible mapping from names to procs)
 */
trait NameServerMsg {}

case class Register(name: String, address: InetAddress, port: Int) extends NameServerMsg
case class Lookup(name: String) extends NameServerMsg

case class Success(name: String, address: InetAddress, port: Int) extends NameServerMsg
case class Failure(name: String) extends NameServerMsg