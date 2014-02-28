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

class ServeLocalNS extends LocalNS {

  NetIO.serverPort(port, 0, false, handleClient).fork

  /**
   * Handle each new client that requests.
   */
  private def handleClient(client: NetIO.Client[Msg, Msg]) = {
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port) =>
          val respCh = OneOne[Boolean]
          toRegistry ! ((name, addr, port, respCh))
          client ! (respCh? match {
            case true => {
              println("Added " + name + " to the registry")
              Success(name, addr, 0)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[(InetAddress, Int)]]
          fromRegistry ! ((name, respCh))
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