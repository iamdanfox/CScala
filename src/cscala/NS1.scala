package cscala
import java.net.InetAddress


/**
 * Guard object ensures that exactly one NameServer for network accessible resources is ever created
 */
object NS1 extends Lookupable[(InetAddress, Int)] with Registerable[(InetAddress,Int)] {
  
    private val ns = new UDPDistributedNS()  
  
    def lookup(name:String) : Option[(InetAddress, Int)] = ns.lookupAddr(name)
    
    def register(name:String, v:(InetAddress, Int), ttl:Long) : Boolean = {
      ns.registerAddr(name, v._1, v._2, ttl)
    }
}



