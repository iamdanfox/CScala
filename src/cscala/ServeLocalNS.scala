package cscala

import java.net.InetAddress

import ox.CSO.OneOne
import ox.CSO.proc
import ox.cso.NetIO

class ServeLocalNS extends LocalNS {

  NetIO.serverPort(NameServer.port, 0, false, handleClient).fork

  /**
   * Handle each new client that requests.
   */
  private def handleClient(client: NetIO.Client[Msg, Msg]) = {
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port, ttl) =>
          val respCh = OneOne[Boolean]
          toRegistry ! ((name, addr, port, ttl, respCh))
          client ! (respCh? match {
            case true => {
              println("Added " + name + " to the registry")
              Success(name, addr, port)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[Record]]
          fromRegistry ! ((name, respCh))
          client ! (respCh? match {
            case Some((addr, port, timestamp, ttl)) => Success(name, addr, port)
            case None => Failure(name)
          })
      }
      // No serve loop
      client.close // TODO: should we really kill off the client after just one request?
    }.fork // TODO: why bother forking?
  }
}