package cscala

import java.net.InetAddress

import cscala.NameServer.Port
import cscala.NameServer.TTL
import ox.CSO.OneOne
import ox.CSO.proc
import ox.CSO.repeat
import ox.cso.Datagram.PortToSocket
import ox.cso.Datagram.SocketToPort
import ox.cso.NetIO


/**
 * TCP accessible nameserver. Forks a process for every client connection
 * 
 */
class ServeLocalNS extends LocalNS {
  
  
  NetIO.serverPort(NameServer.NAMESERVER_PORT, 0, false, handleClient).fork
 

  /**
   * Handle each new client that requests. TODO do we even need to respond to remote lookups?
   */
  private def handleClient(client: NetIO.Client[InterNSMsg, InterNSMsg]) = {
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port, timestamp, ttl) =>
          val respCh = OneOne[Boolean]
          registry.put ! ((name, (addr, port, timestamp, ttl), respCh))
          client ! (respCh? match {
            case true => {
              println("Added " + name + " to the registry")
              Success(name, addr, port)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[Registry.Record]]
          registry.get ! ((name, respCh))
          client ! (respCh? match {
            case Some((addr, port, timestamp, ttl)) => Success(name, addr, port)
            case None => Failure(name)
          })
      }
      // No serve loop
      client.close // TODO: should we really kill off the client after just one request?
    }.fork // TODO: why bother forking?
  }

  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    // attempt insertion
    val rtnCh = OneOne[Boolean]
    val timestamp = System.currentTimeMillis()
    registry.put ! ((name, (address, port, timestamp, ttl), rtnCh))

    return (rtnCh?)
  }
}


/**
 * Messages sent between NameServers of TCP
 */ 
trait InterNSMsg {}

case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp: Registry.Timestamp, ttl: NameServer.TTL) extends InterNSMsg
case class Lookup(name: String) extends InterNSMsg

// these are sent as replies 
case class Success(name: String, address: InetAddress, port: NameServer.Port) extends InterNSMsg
case class Failure(name: String) extends InterNSMsg