package cscala

import java.net.InetAddress

import cscala.NameServer.Port
import cscala.NameServer.TTL
import ox.CSO._
import ox.CSO.alt
import ox.CSO.proc
import ox.CSO.serve
import ox.CSO.repeat
import ox.cso.Datagram.PortToSocket
import ox.cso.Datagram.SocketToPort
import ox.cso.NetIO
import ox.cso.Abort
import ox.cso.Connection.Server
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
 * BEWARE: Constructor blocks until the protocol has finished or timed out.
 */
class UDPDistributedNS(debugname:String="UDPDistributedNS") extends NameServer {

  val registry = new Registry[(InetAddress, Port)]()
  val FILL_TIMEOUT = 1000
  val ANYONE_AWAKE_TIMEOUT = 1000
  
  // set up UDP multicasting =============
  protected def sendMulticast : ?[UDPMessage] with ![UDPMessage] = OneOne[UDPDistributedNS.UDPMessage]
  protected def recvMulticast : ?[UDPMessage] with ![UDPMessage] = OneOne[UDPDistributedNS.UDPMessage]

  /** 
   *  This can be overridden to mock the registry for testing.
   */
  protected def wireUpPortsToSocket() {
    val socket = new java.net.MulticastSocket(UDPDistributedNS.MULTICAST_PORT)
//    socket.joinGroup(InetAddress.getByName("localhost")) // no idea if this is necessary
    val socketAddr = new java.net.InetSocketAddress(InetAddress.getByName("localhost"), UDPDistributedNS.MULTICAST_PORT)
    //  socket.setSoTimeout() // no idea what a normal timeout is

    PortToSocket(sendMulticast, socket, socketAddr) {
      debug("UDPDistributedNS PortToSocket terminated")
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
  
  
  /*
   * Distributed protocol implemented below here.
   */  
  debug(debugname + ": Constructor start")
  // constructor will only start accepting when both of these are finished

  sendMulticast ! AnyoneAwake;
  debug(debugname + ": Sent anyoneawake, listening")

  // essential to use serve because recvMulticast might receive non-OfferFill messages
  // TODO: something broken here!!
  serve(
    recvMulticast ==> {
      case OfferFill(from) => {
        // currently accepts the first offer. TODO, better strategy?
        sendMulticast ! RequestFill(from, nameServerAddress)
        throw new Abort
      }
      case _ => {} // ignore other types.
    } |
      after(ANYONE_AWAKE_TIMEOUT) ==> {
        throw new Abort
      } // TODO: what if we keep receiving non `OfferFill` messages? potential for Livelock here?
  )
    
  debug(debugname + ": Protocol done, starting multicastAdapter")
  
  
  multicastAdapter.fork
  
  


  /** 
   * pipes incoming `Register` messages directly into the registry. no notifications. 
   */
  private def multicastAdapter = proc {
    repeat { true } {
      recvMulticast? {
        case UDPDistributedNS.Register(name,addr,port,timestamp,ttl) => {
          val returnCh = OneOne[Boolean]
          registry.put ! ((name,((addr,port),timestamp,ttl),returnCh))
          val wasUpdated = returnCh?; // TODO should we do something with this data?
          debug(debugname + ": Incoming REGISTER ('"+name+"'), saved="+wasUpdated)
        }
        case Fill(contents) => {
          // TODO should we only accept unsolicited fills?
          print(debugname + ": Incoming FILL: ")
          contents.foreach(r => {
              val rtnCh = OneOne[Boolean]
              registry.put ! ((r.name, ((r.address, r.port), r.timestamp, r.ttl), rtnCh))
              print(rtnCh?);
          })
          debug()
          val retCh = OneOne[Set[(String,registry.Record)]]
          registry.getAll!retCh
          debug(retCh?)
        }
        case AnyoneAwake => {
          debug(debugname + ": Incoming AnyoneAwake, sending OfferFill")
          sendMulticast!OfferFill(this.nameServerAddress)
        }
        case RequestFill(filler, dest) if filler == this.nameServerAddress => {
          val retCh = OneOne[Set[(String,registry.Record)]]
          registry.getAll!retCh;
          val set1 = retCh?;
          if (set1.size > 0 ){
              debug(debugname + ": Incoming RequestFill, sending Fill")   
              val set2 = set1.map( x => { val (n,((a,p),t,ttl)) = x; UDPDistributedNS.Register(n,a,p,t,ttl) })
              sendMulticast!Fill(set2)
          } else {
            // ignore the request if we have nothing to offer!
          }
        }
        // ignore other messages
        case OfferFill(_) => {
          debug(debugname + ": Incoming OfferFill (ignoring)")
        }
        case _ => {}   
      } 
    }
    recvMulticast.close
  }
  
  /**
   * Looks up the name in the registry
   */
  override def lookupAddr(name: String): Option[(InetAddress, Port)] = {
    val rtnCh = OneOne[Option[registry.Record]]
    registry.get ! ((name, rtnCh))
    return (rtnCh?) match {
      case Some((payload, timestamp, ttl)) => Some(payload) // slightly less data returned
      case None => None
    }
  }
  
  override def lookup[Req, Resp](name: String): Option[Server[Req,Resp]] = {
    return lookupAddr(name) match {
      case Some((addr, port)) => Some(NetIO.clientConnection[Req, Resp](addr, port, false)) // synchronous = false
      case None => None
    }
  }

  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerAddr(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    // attempt insertion
    val rtnCh = OneOne[Boolean]
    val timestamp = System.currentTimeMillis()
    registry.put ! ((name, ((address, port), timestamp, ttl), rtnCh))

    // every time an entry is successfully inserted into the registry, we must notify other.
    if (rtnCh?) {
      // successful insertion, so notify other NameServers
      debug(debugname +": Sending '"+name+"'")
      sendMulticast ! UDPDistributedNS.Register(name, address, port, timestamp, ttl) // `Register` member of `Msg` trait
      return true
    } else {
      return false
    }
  }
  
  def debug(x:Any) = Console.println(x:Any)
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
  case class Fill(contents: Set[Register]) extends UDPMessage // TODO include some sort of 'summary' value. Include a `from` attribute. Maybe include a TO?

  case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp: Registry.Timestamp, ttl: NameServer.TTL) extends UDPMessage
}

