package cscala
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._
import java.net.ConnectException

/**
 * Listen
 * Allow services to register (either by listening over a particular port,
 * or through internal JVM communication).
 * Allow processes to lookup services.
 */
object NS {

  private var impl: NameServer = null

  def isRunning(): Boolean = this.impl!=null
  
  // returns a nameserver. constructs a local one if necessary
  /**
   * Returns a NameServer.  Finds one on a local JVM, finds one on this JVM or starts a new one.
   */
  def apply(): NameServer = {
    if (!isRunning()) {
      try {
          System.out.println("NS() trying to connect to local JVM")
          val nameServer = NetIO.clientConnection[Msg, Msg]("localhost", 7700, false)
          // wrap the foreign JVM
          impl = new ForeignNSWrapper(nameServer)
          return impl
      } catch {
        // no nameserver running on a local JVM
        case ce : java.net.ConnectException => {
          println("NS() starting a new local NameServer")
          // start a new one!
          impl = new LocalNS()
          return impl
        }
      } 
    } else {
      // local nameserver is already running
      println("NS() Already running locally")
      impl
    }
  }

  //  def main(args: Array[String]) = {
  //    val ns = NS();
  //  }

  /**
   * Wrapper class that provides a unified interface to a non-local NameServer
   */
  class ForeignNSWrapper(conn: ox.cso.NetIO.Server[Msg,Msg]) extends NameServer {
    
    def register(name: String, address: InetAddress, port: Int): Boolean = {
      conn!Register(name, address, port)
      val resp:Msg = (conn?)
      conn.close
      resp match {
        case Success(n,a,p) => return true
        case Failure(n) => return false
      }
    }
    
    def lookup(name: String): Option[(InetAddress, Int)] = {
      if (!conn.isOpen()) println("conn not open")
      conn!Lookup(name)
      val resp:Msg = (conn?) 
      conn.close
      resp match {
        case Success(n,a,p) => return Some(a,p)
        case Failure(n) => return None
      }
    }
  }
  
  /**
   * Stores a mapping from String names -> (InetAddress, Int). 
   * Exposed internally through `register` and `lookup` methods and over the network on NameServer.port (7700)
   */
  class LocalNS extends NameServer {

    private val hashmap = new scala.collection.mutable.HashMap[String, (InetAddress, Int)]();
    private val putCh = ManyOne[(String, InetAddress, Int, OneOne[Boolean])]
    private val getCh = ManyOne[(String, OneOne[Option[(InetAddress,Int)]])]

    // Constructor: spawns hashmap guard proc and server
    registry().fork
    NetIO.serverPort(port, 0, false, handler).fork
    
    /**
     * Add a new mapping from String -> (InetAddress, Port)
     */
    def register(name : String, address: InetAddress, port: Int) : Boolean = {
      val rtnCh = OneOne[Boolean]
      putCh!((name,address,port,rtnCh))
      return rtnCh?
    }
    
    /**
     * Looks up the name in the registry
     */
    def lookup(name : String) : Option[(InetAddress,Int)] = {
      val rtnCh = OneOne[Option[(InetAddress,Int)]]
      getCh!((name,rtnCh))
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
              hashmap.put(name,(addr,port))
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
            val respCh = OneOne[Option[(InetAddress,Int)]]
            getCh ! ((name, respCh))
            client ! (respCh? match {
              case Some((addr,port)) => Success(name, addr, port) 
              case None => Failure(name)
            })
        }
        // No serve loop
        client.close
      }.fork // TODO: why bother forking?
    }
  }

  //    // dummy insertion
  //    (proc {
  //      val respCh = OneOne[Boolean];
  //      // TODO: PORT!
  //      putCh ! (("DummyService", InetAddress.getByName("localhost"), respCh))
  //      println("Entered DummyService: " + (respCh?))
  //    })();

}