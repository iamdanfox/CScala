package cscala

import java.net.InetAddress

import cscala.NameServer.Port
import cscala.NameServer.TTL
import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO.serve
import cscala.Registry._



object Registry {
  // IP Address, Port, Timestamp, TTL
  type Record = (InetAddress, Port, Timestamp, TTL)
  type Timestamp = Long
}


class Registry {
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  val put = ManyOne[(String, InetAddress, Port, Timestamp, TTL, OneOne[Boolean])] 
  val get = ManyOne[(String, OneOne[Option[Record]])] // used to do lookups
  
  guardProc().fork
  
  /**
   * Registry protects the hashmap, ensuring no race conditions
   * Maintains the TTL invariant. Only returns valid records.
   * Only accepts records with more recent timestamps.
   */
  private def guardProc() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    // allows two possible operations. PUT (on toRegistry) and GET (on fromRegistry)
    serve(
      put ==> {
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
      | get ==> {
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
    put.close; get.close;
  }
}