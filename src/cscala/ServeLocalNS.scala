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
 * Functions as part of a system of nameservers, each maintaining the same state.  Hence, it must respond 
 * to all the message types:
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
          registry.put ! ((name, addr, port, timestamp, ttl, respCh))
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
    registry.put ! ((name, address, port, timestamp, ttl, rtnCh))

    // every time an entry is successfully inserted into the registry, we must notify other.
    if (rtnCh?) {
      // successful insertion, so notify other NameServers
      println(" ServeLocalNS: Sending '"+name+"'")
      sendMulticast ! Register(name, address, port, timestamp, ttl) // `Register` member of `Msg` trait
      return true
    } else {
      return false
    }
  }

  // set up UDP multicasting =============
  val sendMulticast = OneOne[Register]
  val recvMulticast = new OneOne[Register]

  val socket = new java.net.MulticastSocket(ServeLocalNS.MULTICAST_PORT)
  //   socket.joinGroup(InetAddress.getByName("localhost")) // no idea if this is correct.
  val socketAddr = new java.net.InetSocketAddress(InetAddress.getByName("localhost"), ServeLocalNS.MULTICAST_PORT)
  //  socket.setSoTimeout() // no idea what a normal timeout is

  PortToSocket(sendMulticast, socket, socketAddr) {
    println("ServerLocalNS PortToSocket terminated")
    if (!socket.isClosed()) socket.close()
    sendMulticast.close
  }.withName("ServeLocalNS Multicast PortToSocket").fork

  // listen for UDP broadcasts =============

  // TODO. recover from buffer overflow
  SocketToPort(ServeLocalNS.MAX_MCAST_LENGTH, socket, recvMulticast) {
    // TODO: recursive initialisation?
    if (!socket.isClosed()) socket.close()
    recvMulticast.close
  }.withName("ServerLocalNS Multicast SocketToPort").fork

  adapter.fork
  
  // pipes incoming multicast `Register` messages directly into the registry. no notifications.
  private def adapter = proc {
    repeat { true } {
      recvMulticast? {
        case Register(name,addr,port,timestamp,ttl) =>
          val returnCh = OneOne[Boolean]
          registry.put ! ((name,addr,port,timestamp,ttl,returnCh))
          print(" ServeLocalNS: Receiving '"+name+"', saved=")
          println (returnCh?) // TODO should we do something with this data?
      } 
    }
  }
}

object ServeLocalNS {
  val MULTICAST_PORT = 3303
  val MAX_MCAST_LENGTH = 65535 // TODO enforce some limit on outbound messages
}


/**
 * these messages are broadcast over UDP to maintain consistency.
 */ 
trait InterNSMsg {}

/**
 * Broadcast when a new NameServer starts up, with an empty registry
 */
object AnyoneAwake extends InterNSMsg

/**
 * Other registries offer to fill the new nameserver
 */
case class OfferFill() extends InterNSMsg // TODO from IP??

/**
 * The new nameserver selects one particular
 */
//case class 


case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp: Registry.Timestamp, ttl: NameServer.TTL) extends InterNSMsg
case class Lookup(name: String) extends InterNSMsg

case class Success(name: String, address: InetAddress, port: NameServer.Port) extends InterNSMsg
case class Failure(name: String) extends InterNSMsg