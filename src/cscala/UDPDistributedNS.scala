package cscala

import java.net.InetAddress

import cscala.NameServer.Port
import cscala.NameServer.TTL
import ox.CSO._
import ox.CSO.proc
import ox.CSO.serve
import ox.CSO.repeat
import ox.cso.Datagram.PortToSocket
import ox.cso.Datagram.SocketToPort
import ox.cso.NetIO
import cscala.UDPDistributedNS._

/**
 * Functions as part of a system of nameservers, each maintaining the same state over UDP.  
 * 
 * On startup, send an "AnyoneAwake" message out over UDP. Subject to a timeout, listen for "OfferFill"
 * messages. Respond to one of them with a "RequestFill" message.  Wait for a "Fill" message to prepopulate the registry.
 * 
 * Then start responding to `registerForeign` requests.
 * 
 * Also broadcast updates over UDP. 
 * 
 * 
 */
class UDPDistributedNS extends NameServer {

  val registry = new Registry()
  
  // set up UDP multicasting =============
  protected def sendMulticast : ?[UDPMessage] with ![UDPMessage] = OneOne[UDPDistributedNS.UDPMessage]
  protected def recvMulticast : ?[UDPMessage] with ![UDPMessage] = OneOne[UDPDistributedNS.UDPMessage]

  /** 
   *  This can be overridden to mock the registry for testing.
   */
  protected def wireUpPortsToSocket() {
    val socket = new java.net.MulticastSocket(UDPDistributedNS.MULTICAST_PORT)
    socket.joinGroup(InetAddress.getByName("localhost")) // no idea if this is necessary
    val socketAddr = new java.net.InetSocketAddress(InetAddress.getByName("localhost"), UDPDistributedNS.MULTICAST_PORT)
    //  socket.setSoTimeout() // no idea what a normal timeout is

    PortToSocket(sendMulticast, socket, socketAddr) {
      println("UDPDistributedNS PortToSocket terminated")
      if (!socket.isClosed()) socket.close()
      sendMulticast.close
    }.withName("UDPDistributedNS Multicast PortToSocket").fork

    // listen for UDP broadcasts =============

    // TODO. recover from buffer overflow
    SocketToPort(UDPDistributedNS.MAX_MCAST_LENGTH, socket, recvMulticast) {
      // TODO: recursive initialisation?
      if (!socket.isClosed()) socket.close()
      recvMulticast.close
    }.withName("UDPDistributedNS Multicast SocketToPort").fork
  }
  wireUpPortsToSocket()
  
  
  val offerChosen = OneOne[InetAddress]
  
  val fillListener = proc {
    sendMulticast!AnyoneAwake;
    serve(
      recvMulticast ==> {
        case OfferFill(from) => println(from)
      } 
      | offerChosen -!-> {
        offerChosen!InetAddress.getLocalHost(); // TODO replace this
      }
    )
  }
  
  val fillOfferController = proc{
    Thread.sleep(1000);
    val selected = offerChosen?;
    //   accept one offer
    sendMulticast!RequestFill(selected, nameServerAddress)

    // receive a 'Fill' message; discarding other types.
    serve(
      recvMulticast ==> {
        case Fill(contents) => {
          contents.foreach( r => {
            val rtnCh = OneOne[Boolean]
            registry.put!((r.name, r.address, r.port, r.timestamp, r.ttl, rtnCh))
            rtnCh?;
          })
          // once the registry has been filled, start up and start accepting requests.
          // TODO
        }
        case _ => println("ignore other type")
      }
    )
    

  }
  
  val actualRegistry = proc {
    // wait for the registry to be filled
    // maybe use semaphores!!!
    // TODO
  }
  
  (fillOfferController || fillListener || actualRegistry)()
  
  
  
  
  
  
  

  multicastAdapter.fork

  /** 
   * pipes incoming `Register` messages directly into the registry. no notifications. 
   */
  private def multicastAdapter = proc {
    repeat { true } {
      recvMulticast? {
        case UDPDistributedNS.Register(name,addr,port,timestamp,ttl) =>
          val returnCh = OneOne[Boolean]
          registry.put ! ((name,addr,port,timestamp,ttl,returnCh))
          print(" UDPDistributedNS: Receiving '"+name+"', saved=")
          val wasUpdated = returnCh?; 
          println(wasUpdated) // TODO should we do something with this data?
      } 
    }
    recvMulticast.close
  }
  
  /**
   * Looks up the name in the registry
   */
  override def lookupForeign(name: String): Option[(InetAddress, Port)] = {
    val rtnCh = OneOne[Option[Registry.Record]]
    registry.get ! ((name, rtnCh))
    return (rtnCh?) match {
      case Some((addr,port, timestamp, ttl)) => Some (addr,port) // slightly less data returned
      case None => None
    }
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
      println(" UDPDistributedNS: Sending '"+name+"'")
      sendMulticast ! UDPDistributedNS.Register(name, address, port, timestamp, ttl) // `Register` member of `Msg` trait
      return true
    } else {
      return false
    }
  }
}

object UDPDistributedNS {
  val MULTICAST_PORT = 3303
  val MAX_MCAST_LENGTH = 65507 // TODO enforce some limit on outbound messages (single packet)

  /**
   * these messages are broadcast over UDP to maintain consistency.
   */
  trait UDPMessage {}

  /**
   * Broadcast when a new NameServer starts up, with an empty registry
   */
  object AnyoneAwake extends UDPMessage

  /**
   * Other registries offer to fill the new nameserver
   */
  case class OfferFill(from: InetAddress) extends UDPMessage // TODO: include some sort of 'summary' of registry state, describe originating

  /**
   * The new nameserver selects one particular
   */
  case class RequestFill(filler: InetAddress, fillee: InetAddress) extends UDPMessage

  /**
   * The selected nameserver sends the contents of the registry across.
   */
  case class Fill(contents: Set[Register]) extends UDPMessage // TODO include some sort of 'summary' value

  case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp: Registry.Timestamp, ttl: NameServer.TTL) extends UDPMessage
}

