package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._

trait NameServer {
  def register(name: String, address: InetAddress, port: Int): Boolean
  def lookup(name: String): Option[InetAddress]

  // deregister?
}

/**
 * Listen
 * Allow services to register (either by listening over a particular port,
 * or through internal JVM communication).
 * Allow processes to lookup services.
 */
object NS {

  // construct if necessary, otherwise just return the singleton
  def apply(): NameServer = {
    if (impl == null) impl = new Impl()
    return impl;
  }

  private var impl: NameServer = null

  class Impl extends NameServer {

    val port = 7700;
    private val hashmap = new scala.collection.mutable.HashMap[String, (InetAddress, Int)]();
    private val putCh = ManyOne[(String, InetAddress, Int, OneOne[Boolean])]
    private val getCh = ManyOne[(String, OneOne[Option[(InetAddress,Int)]])]
    
    def Impl() {
      // spawn server and hashmap guard proc
      registry(putCh, getCh).fork
      NetIO.serverPort(port, 0, false, handler).fork
    }

    def register(name: String, address: InetAddress, port: Int): Boolean = {
      return false;
    }

    def lookup(name: String): Option[InetAddress] = {
      return null;
    }

    /**
     * registry maintains a Name -> InetAddress, ensuring no race conditions
     */
    private def registry(putCh: ManyOne[(String, InetAddress, Int, OneOne[Boolean])],
      getCh: ManyOne[(String, OneOne[Option[(InetAddress,Int)]])]) = proc {
      println("registry started")
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
  }

  //  type Name = String
  //  
  //  
  //  def register(name : String, address: InetAddress, port: Int) : Boolean = {
  //    val rtnCh = OneOne[Boolean]
  //    putCh!((name,address,port,rtnCh))
  //    return rtnCh?
  //  }
  //  
  //  def lookup(name : String) : Option[InetAddress] = {
  //    val rtnCh = OneOne[Option[InetAddress]]
  //    getCh!((name,rtnCh))
  //    return rtnCh?
  //  }
  //  
  //  // publically accessible for intra JVM communication
  //  
  //  
  //  
  //  private var registryThreadRunning = false
  //
  //  /**
  //   * Ensures that a single registry process is running. NOOP if already running.
  //   */
  //  private def startRegistry(){
  //    // if registry has already been forked, don't do anything.
  //    if (!registryThreadRunning){
  //      registry(putCh,getCh).fork
  //      registryThreadRunning = true
  //    }
  //  }
  //  
  //  private var serverRunning = false;
  //  
  //  private def startServer(){
  //    if (!serverRunning){
  //        NetIO.serverPort(port, 0, false, handler).fork
  //        serverRunning = true
  //    }
  //  }
  //  
  //  def start(){
  //    startRegistry()
  //    startServer()
  //  }
  //  
  //  def main(args: Array[String]) = {
  //    println("starting")
  //    
  //    start()
  //
  //    // dummy insertion
  //    (proc {
  //      val respCh = OneOne[Boolean];
  //      // TODO: PORT!
  //      putCh ! (("DummyService", InetAddress.getByName("localhost"), respCh))
  //      println("Entered DummyService: " + (respCh?))
  //    })();
  //
  //    println("all started")
  //  }
  //
  //  
  //
  //  private def handler(client: NetIO.Client[NameServerMsg, NameServerMsg]) = {
  //    proc("NameServer handler for " + client.socket) {
  //      // react appropriately to first message, then close
  //      client? match {
  //        case Register(name, addr, port) =>
  //          val respCh = OneOne[Boolean]
  //          putCh ! ((name, addr, respCh))
  //          client ! (respCh? match {
  //            case true => {
  //              println("Added "+name+" to the registry")
  //              Success(name, addr, 0)
  //            }
  //            case false => Failure(name)
  //          })
  //        case Lookup(name) =>
  //          val respCh = OneOne[Option[InetAddress]]
  //          getCh ! ((name, respCh))
  //          client ! (respCh? match {
  //            case Some(addr) => Success(name, addr, 0) // TODO port
  //            case None => Failure(name)
  //          })
  //      }
  //      // No serve loop
  //      client.close
  //    }.fork // TODO: why bother forking?
  //  }
}
