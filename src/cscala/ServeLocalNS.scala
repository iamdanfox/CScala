package cscala

import java.net.InetAddress

import ox.CSO.OneOne
import ox.CSO.proc
import ox.cso.NetIO
import cscala.NameServer._

class ServeLocalNS extends LocalNS {

  NetIO.serverPort(NameServer.NAMESERVER_PORT, 0, false, handleClient).fork

  /**
   * Handle each new client that requests.
   */
  private def handleClient(client: NetIO.Client[Msg, Msg]) = {
    proc("NameServer handler for " + client.socket) {
      // react appropriately to first message, then close
      client? match {
        case Register(name, addr, port, timestamp, ttl) =>
          val respCh = OneOne[Boolean]
          toRegistry ! ((name, addr, port, timestamp, ttl, respCh)) 
          client ! (respCh? match {
            case true => {
              println("Added " + name + " to the registry")
              Success(name, addr, port)
            }
            case false => Failure(name)
          })
        case Lookup(name) =>
          val respCh = OneOne[Option[LocalNS.Record]]
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
  
  
  /**
   * Add a new mapping from String -> (InetAddress, Port)
   */
  override def registerForeign(name: String, address: InetAddress, port: Port, ttl: TTL): Boolean = {
    val rtnCh = OneOne[Boolean]
    val timestamp = System.currentTimeMillis()
    toRegistry ! ((name, address, port, timestamp, ttl, rtnCh))
    // every time an entry is successfully inserted into the registry, we must notify others.
    return rtnCh?
  }
  
  
//  
////   TODO: listen on some port for UDP broadcasts... update registry.
//  
//  private def notifyOthers(name:String, address:InetAddress,port:Port,ttl:TTL) {
//    // UDP flooding.
////    http://www.barricane.com/udp-echo-server-scala.html
//  }
}

trait Msg {}

case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp:LocalNS.Timestamp, ttl: NameServer.TTL) extends Msg
case class Lookup(name: String) extends Msg

case class Success(name: String, address: InetAddress, port: NameServer.Port) extends Msg
case class Failure(name: String) extends Msg