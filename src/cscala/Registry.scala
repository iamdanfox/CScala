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
  type Timestamp = Long
}


class Registry { // TODO refactor this into a trait & HashRegistry
  // IP Address, Port, Timestamp, TTL
  type Record = ((InetAddress, Port), Timestamp, TTL)
  
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  val put = ManyOne[(String, Record, OneOne[Boolean])] 
  val get = ManyOne[(String, OneOne[Option[Record]])] // used to do lookups
  val getAll = ManyOne[OneOne[Set[(String,Record)]]]
  
  /**
   * Send a unit () to this channel to terminate the Registry.  Note, the thread will only
   * terminate when the threadpool is all done.
   */
  val terminate = OneOne[Unit]
  
  guardProc().fork
  
  /**
   * Registry protects the hashmap, ensuring no race conditions
   * Maintains the TTL invariant. Only returns valid records.
   * Only accepts records with more recent timestamps.
   */
  private def guardProc() = proc { // started when class is constructed
    // allows two possible operations. PUT (on toRegistry) and GET (on fromRegistry)
    serve(
      put ==> {
        case (name, (payload, newTimestamp, ttl), rtn) =>
          // only accept record if the timestamp is more recent
          rtn ! (hashmap.get(name) match {
            case Some((_, existingTimestamp, _)) if newTimestamp < existingTimestamp =>
              false // no update
            case _ =>
              hashmap.put(name, (payload, newTimestamp, ttl)) // new or updated record.
              true
          })
      }
      | get ==> {
        case (n, rtn) =>
            // only return valid records
            hashmap.get(n) match {
              case Some((payload, timestamp, ttl)) => {
                if (timestamp + ttl > System.currentTimeMillis()) {
                  rtn ! Some(payload, timestamp, ttl)
                } else {
                  hashmap.remove(n) // expired record
                  rtn ! None
                }
              }
              case None => rtn ! None
            }
      }
      | getAll ==> { returnCh =>
        returnCh!hashmap.toSet[(String,Record)]
        returnCh.close
      }
      | terminate ==> {_ => throw new ox.cso.Abort}
    )
    put.close; get.close;
  }
}