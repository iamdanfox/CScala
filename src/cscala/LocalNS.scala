package cscala
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._
import java.net.ConnectException
import java.net.InetAddress

/**
 * Stores a mapping from String names -> (InetAddress, Int).
 * Exposed internally through `register` and `lookup` methods and over the network on NameServer.port (7700)
 */
class LocalNS extends NameServer {

  private val hashmap = new scala.collection.mutable.HashMap[String, (InetAddress, Int)]();
  private val putCh = ManyOne[(String, InetAddress, Int, OneOne[Boolean])]
  private val getCh = ManyOne[(String, OneOne[Option[(InetAddress, Int)]])]

  // Constructor: spawns hashmap guard proc and server
  registry().fork
  NetIO.serverPort(port, 0, false, handler).fork

  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  def register(name: String, address: InetAddress, port: Int): Boolean = {
    val rtnCh = OneOne[Boolean]
    putCh ! ((name, address, port, rtnCh))
    return rtnCh?
  }

  /**
   * Looks up the name in the registry
   */
  def lookup(name: String): Option[(InetAddress, Int)] = {
    val rtnCh = OneOne[Option[(InetAddress, Int)]]
    getCh ! ((name, rtnCh))
    return rtnCh?
  }

  /**
   * Registry maintains (Name -> (InetAddress,Int)), ensuring no race conditions
   */
  private def registry() = proc { // started when class is constructed
    println("LocalNS: starting a registry")
    serve(
      putCh ==> {
        case (name, addr, port, rtn) =>
          if (hashmap.contains(name))
            rtn ! false
          else {
            hashmap.put(name, (addr, port))
            rtn ! true
          }
      } | getCh ==> {
        case (n, rtn) => rtn ! hashmap.get(n)
      })
    putCh.close; getCh.close;
  }

  /**
   * Handle each new client that requests.
   */
  private def handler(client: NetIO.Client[Msg, Msg]) = { // passed into NetIO.serverPort
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port) =>
          val respCh = OneOne[Boolean]
          putCh ! ((name, addr, port, respCh))
          client ! (respCh? match {
            case true => {
              println("Added " + name + " to the registry")
              Success(name, addr, 0)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[(InetAddress, Int)]]
          getCh ! ((name, respCh))
          client ! (respCh? match {
            case Some((addr, port)) => Success(name, addr, port)
            case None => Failure(name)
          })
      }
      // No serve loop
      client.close
    }.fork // TODO: why bother forking?
  }
}