package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO._
import cscala.NameServer._

/**
 * Stores a mapping from String names -> (InetAddress, Int). 
 * Doesn't allow duplicate records for one name
 * Exposed internally through `register` and `lookup` methods and over the network on NameServer.port (7700)
 */
class LocalNS extends NameServer {

  // IP Address, Port, Timestamp, TTL
  type Record = (InetAddress, Port, Timestamp, TTL)
  type Timestamp = Long
  
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  protected val toRegistry = ManyOne[(String, InetAddress, Port, TTL, OneOne[Boolean])] 
  protected val fromRegistry = ManyOne[(String, OneOne[Option[Record]])] // used to do lookups

  // Constructor: spawns hashmap guard proc
  registry().fork
  
  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    val rtnCh = OneOne[Boolean]
    toRegistry ! ((name, address, port, ttl, rtnCh))
    return rtnCh?
  }

  /**
   * Looks up the name in the registry
   */
  override def lookupForeign(name: String): Option[(InetAddress, Port)] = {
    val rtnCh = OneOne[Option[Record]]
    fromRegistry ! ((name, rtnCh))
    return (rtnCh?) match {
      case Some((addr,port, timestamp, ttl)) => Some (addr,port) // slightly less data returned
      case None => None
    }
  }

  /**
   * Registry maintains (Name -> (InetAddress,Int)), ensuring no race conditions
   * Maintains the TTL invariant. Only returns valid records.
   */
  private def registry() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    serve(
      toRegistry ==> {
        case (name, addr, port, ttl, rtn) =>
            hashmap.put(name, (addr, port, System.currentTimeMillis(), ttl)) 
            rtn ! true
      } 
      | fromRegistry ==> {
        case (n, rtn) =>
          hashmap.get(n) match {
            case Some((addr, port, timestamp, ttl)) => {
              if (timestamp + ttl > System.currentTimeMillis()) {
                rtn ! Some(addr, port, timestamp, ttl) // slightly less data returned
              } else {
                hashmap.remove(n) // expired record
                rtn ! None
              }
            }
            case None => rtn ! None
          }
      }
  )
    toRegistry.close; fromRegistry.close;
  }
}