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

  // IP Address, Port, TTL
  type Record = (InetAddress, Int, Long)

  case class Rcrd(addr: InetAddress, port: Int, ttl:Long)
  
  private val hashmap = new scala.collection.mutable.HashMap[String, Record]();
  
  protected val toRegistry = ManyOne[(String, InetAddress, Int, Long, OneOne[Boolean])]
  protected val fromRegistry = ManyOne[(String, OneOne[Option[(InetAddress, Int, Long)]])]

  // Constructor: spawns hashmap guard proc
  registry().fork

  // start reaper.
  val stopReaper = OneOne[Unit]
  reaper(stopReaper).fork
  
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
    val rtnCh = OneOne[Option[(InetAddress, Int, Long)]]
    fromRegistry ! ((name, rtnCh))
    return rtnCh? match {
      case Some((a,p,t)) => Some (a,p) // slightly less data returned
      case None => None
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
          if (hashmap.get(name) == Some((addr,port,ttl)) ) // TODO no updates
            rtn ! false
          else {
            hashmap.put(name, (addr, port, ttl))
            rtn ! true
          }
      } | fromRegistry ==> {
        case (n, rtn) => rtn ! hashmap.get(n)
      })
    // TODO: when would this serve loop even terminate?
    toRegistry.close; fromRegistry.close;
  }
  
  val REAPER_INTERVAL = 1000*3; // every 3 seconds

  /**
   * Every 3 seconds, this scans the `expires` list and deletes any expired records from the NameServer
   */
  private def reaper(stopCh: ?[Unit]) = proc {
    serve(
        stopCh ==> { _ => ox.CSO.Stop }
      | after(REAPER_INTERVAL) --> { 
        val now = System.currentTimeMillis()
        print("reaping "+now+" :")
        hashmap.filter(t => t._2._3 < now).foreach(t => hashmap.remove(t._1));
        println(hashmap.size)
      }
    )
    stopCh.closein
  }
}