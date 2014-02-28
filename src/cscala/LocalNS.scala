package cscala

import java.net.InetAddress

import ox.CSO.ManyOne
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO.serve

/**
 * Stores a mapping from String names -> (InetAddress, Int).
 * Exposed internally through `register` and `lookup` methods and over the network on NameServer.port (7700)
 */
class LocalNS extends NameServer {

  private val hashmap = new scala.collection.mutable.HashMap[String, (InetAddress, Int)]();
  protected val toRegistry = ManyOne[(String, InetAddress, Int, OneOne[Boolean])]
  protected val fromRegistry = ManyOne[(String, OneOne[Option[(InetAddress, Int)]])]

  // Constructor: spawns hashmap guard proc
  registry().fork

  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  def register(name: String, address: InetAddress, port: Int): Boolean = {
    val rtnCh = OneOne[Boolean]
    toRegistry ! ((name, address, port, rtnCh))
    return rtnCh?
  }

  /**
   * Looks up the name in the registry
   */
  def lookup(name: String): Option[(InetAddress, Int)] = {
    val rtnCh = OneOne[Option[(InetAddress, Int)]]
    fromRegistry ! ((name, rtnCh))
    return rtnCh?
  }

  /**
   * Registry maintains (Name -> (InetAddress,Int)), ensuring no race conditions
   */
  private def registry() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    serve(
      toRegistry ==> {
        case (name, addr, port, rtn) =>
          if (hashmap.contains(name))
            rtn ! false
          else {
            hashmap.put(name, (addr, port))
            rtn ! true
          }
      } | fromRegistry ==> {
        case (n, rtn) => rtn ! hashmap.get(n)
      })
    // TODO: when would this serve loop even terminate?
    toRegistry.close; fromRegistry.close;
  }
}