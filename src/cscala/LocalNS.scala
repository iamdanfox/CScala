package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO._

/**
 * Stores a mapping from String names -> (InetAddress, Int). 
 * Doesn't allow duplicate records for one name
 * Exposed internally through `register` and `lookup` methods and over the network on NameServer.port (7700)
 */
class LocalNS extends NameServer {

  // IP Address, Port, ExpiryTime (milliseconds)
  type Record = (InetAddress, Int, Long)
  
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  
  protected val toRegistry = ManyOne[(String, InetAddress, Int, Long, OneOne[Boolean])] // used to make new entries (Long = TTL)
  protected val fromRegistry = ManyOne[(String, OneOne[Option[Record]])] // used to do lookups

  // Constructor: spawns hashmap guard proc
  registry().fork
  
  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Int, ttl: Long): Boolean = {
    val rtnCh = OneOne[Boolean]
    toRegistry ! ((name, address, port, ttl, rtnCh))
    return rtnCh?
  }

  /**
   * Looks up the name in the registry
   */
  override def lookupForeign(name: String): Option[(InetAddress, Int)] = {
    val rtnCh = OneOne[Option[Record]]
    fromRegistry ! ((name, rtnCh))
    rtnCh? match {
      case Some((addr,port,expiry)) =>{
        if (expiry > System.currentTimeMillis()) {
          return Some (addr,port) // slightly less data returned
        } else { 
          hashmap.remove(name) // expired record
          return None
        }
      } 
      case None => return None
    }
  }

  /**
   * Registry maintains (Name -> (InetAddress,Int)), ensuring no race conditions
   */
  private def registry() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    serve(
      toRegistry ==> {
        case (name, addr, port, ttl, rtn) =>
            val expiryTime = System.currentTimeMillis() + ttl
            hashmap.put(name, (addr, port, expiryTime)) // important: hashmap doesn't store TTL!!
            rtn ! true
      } | fromRegistry ==> {
        case (n, rtn) => rtn ! hashmap.get(n)
      })
    // TODO: when would this serve loop even terminate?
    toRegistry.close; fromRegistry.close;
  }
}