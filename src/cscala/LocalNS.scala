package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO._
import cscala.NameServer._
import cscala.LocalNS._


object LocalNS {
  // IP Address, Port, Timestamp, TTL
  type Record = (InetAddress, Port, Timestamp, TTL)
  type Timestamp = Long
}


class LocalNS extends NameServer {
  
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  protected val toRegistry = ManyOne[(String, InetAddress, Port, Timestamp, TTL, OneOne[Boolean])] 
  protected val fromRegistry = ManyOne[(String, OneOne[Option[Record]])] // used to do lookups

  // Constructor: spawns hashmap guard proc
  registry().fork
  
  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    val rtnCh = OneOne[Boolean]
    val timestamp = System.currentTimeMillis()
    toRegistry ! ((name, address, port, timestamp, ttl, rtnCh))
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
   * Registry protects the hashmap, ensuring no race conditions
   * Maintains the TTL invariant. Only returns valid records.
   * Only accepts records with more recent timestamps.
   */
  private def registry() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    // allows two possible operations. PUT (on toRegistry) and GET (on fromRegistry)
    serve(
      toRegistry ==> {
        case (name, addr, port, newTimestamp, ttl, rtn) =>
          // only accept record if the timestamp is more recent
          rtn ! (hashmap.get(name) match {
            case Some((_, _, existingTimestamp, _)) if newTimestamp < existingTimestamp =>
              false // no update
            case _ =>
              hashmap.put(name, (addr, port, newTimestamp, ttl)) // new or updated record.
              true
          })
      }
      | fromRegistry ==> {
        case (n, rtn) =>
            // only return valid records
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