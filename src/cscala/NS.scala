package cscala
import java.net.InetAddress
import ox.cso.Connection._

/**
 * Guard object ensures that exactly one UDPDistributedNS for network accessible resources is ever created
 */
object NS extends Connectable {

  protected val ns = new UDPDistributedNS()

  val nameServerAddress = InetAddress.getLocalHost()

  def lookup(name: String): Option[(InetAddress, Int)] =
    ns.lookupAddr(name)

  def connect[Req, Resp](name: String): Option[Server[Req, Resp]] =
    ns.lookup[Req, Resp](name)

  def register(name: String, v: (InetAddress, Int), ttl: Long): Boolean =
    ns.registerAddr(name, v._1, v._2, ttl)
    
  def remove(name: String) =
    ns.registerAddr(name, null.asInstanceOf[InetAddress], null.asInstanceOf[Int], 0)
}

/**
 * Guard object ensures that exactly one FullyLocalNS for *locally* accessible resources is ever created
 */
object LocalNS extends Connectable {

  protected val ns = new FullyLocalNS()

  def connect[Req, Resp](name: String): Option[Server[Req, Resp]] =
    ns.connect[Req, Resp](name)

  def register[Req, Resp](name: String, handleClient: Client[Req, Resp] => Unit, ttl: Long): Boolean =
    ns.register[Req, Resp](name, ttl, handleClient)
    
  def remove(name: String) =
    ns.register(name, 0, null.asInstanceOf[Client[Nothing,Nothing] => Unit])
}

object DualNS extends Connectable {

  /**
   * Does local lookup first, then attempts a foreign one.
   */
  def connect[Req, Resp](name: String): Option[Server[Req, Resp]] =
    LocalNS.connect[Req, Resp](name) match {
      case Some(s) => Some(s)
      case None => NS.connect[Req, Resp](name)
    }

  def register[Req, Resp](name: String, handleClient: Client[Req, Resp] => Unit, ttl: Long): Boolean =
    LocalNS.register[Req, Resp](name, handleClient, ttl)

  def register(name: String, v: (InetAddress, Int), ttl: Long): Boolean =
    NS.register(name, v, ttl)
}