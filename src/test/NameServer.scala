package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._

/**
 * Listen on a specific port.
 * Allow services to register
 * Allow processes to lookup services.
 */
object NameServer {

  val port = 7700;

  type Name = String;

  val putCh = ManyOne[(Name, InetAddress, OneOne[Boolean])]
  val getCh = ManyOne[(Name, OneOne[Option[InetAddress]])]

  def main(args: Array[String]) = {
    println("starting")
    registry().fork

    (proc {
      val respCh = OneOne[Boolean];
      // TODO: PORT!
      putCh ! (("DummyEntry", InetAddress.getByName("localhost"), respCh))
      println("Entered DummyEntry: " + (respCh?))
    })();
    // (behaves correctly when a duplicate name attempt is made)

    NetIO.serverPort(port, 0, false, handler).fork
    println("all started")
  }

  /**
   * registry ensures no race conditions
   */
  private def registry() = {
    // TODO: registry should store the port
    val hashmap = new scala.collection.mutable.HashMap[Name, InetAddress](); // should it also store timestamp of insertion?
    serve(
      putCh ==> {
        case (n, i, ch) =>
          if (hashmap.contains(n)) ch ! false
          else {
            hashmap.put(n, i)
            ch ! true
          }
      } | getCh ==> {
        case (n, ch) => ch ! hashmap.get(n)
      })
    putCh.close; getCh.close;
  }

  private def handler(client: NetIO.Client[NameServerMsg, NameServerMsg]) = {
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port) =>
          val respCh = OneOne[Boolean]
          putCh ! ((name, addr, respCh))
          client ! (respCh? match {
            case true => {
              println("Added "+name+" to the registry")
              Success(name, addr, 0)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[InetAddress]]
          getCh ! ((name, respCh))
          client ! (respCh? match {
            case Some(addr) => Success(name, addr, 0) // TODO port
            case None => Failure(name)
          })
      }
      // No serve loop
      client.close
    }.fork
  }
}