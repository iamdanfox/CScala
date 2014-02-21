package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._

/**
 * Listen
 * Allow services to register (either by listening over a particular port, 
 * or through internal JVM communication).
 * Allow processes to lookup services.
 */
object NameServer {


  val port = 7700;
  type Name = String;
  
    // publically accessible for intra JVM communication
  val putCh = ManyOne[(Name, InetAddress, OneOne[Boolean])]
  val getCh = ManyOne[(Name, OneOne[Option[InetAddress]])]
  
  private var registryThreadRunning = false

  /**
   * Ensures that a single registry process is running. NOOP if already running.
   */
  private def startRegistry(){
    // if registry has already been forked, don't do anything.
    if (!registryThreadRunning){
      registry(putCh,getCh).fork
      registryThreadRunning = true
    }
  }
  
  private var serverRunning = false;
  
  private def startServer(){
    if (!serverRunning){
        NetIO.serverPort(port, 0, false, handler).fork
        serverRunning = true
    }
  }
  
  def start(){
    startRegistry()
    startServer()
  }
  
  def main(args: Array[String]) = {
    println("starting")
    
    start()

    // dummy insertion
    (proc {
      val respCh = OneOne[Boolean];
      // TODO: PORT!
      putCh ! (("DummyEntry", InetAddress.getByName("localhost"), respCh))
      println("Entered DummyEntry: " + (respCh?))
    })();

    println("all started")
  }

  /**
   * registry maintains a Name -> InetAddress, ensuring no race conditions
   */
  private def registry(putCh: ManyOne[(Name, InetAddress, OneOne[Boolean])], 
      getCh: ManyOne[(Name, OneOne[Option[InetAddress]])]) = proc {
    println("registry started")
    // TODO: registry should store the port, too
    // TODO: should you be able to terminate the registry?
    val hashmap = new scala.collection.mutable.HashMap[Name, InetAddress](); // should it also store timestamp of insertion?
    serve(
      putCh ==> {
        case (n, i, rtn) =>
          if (hashmap.contains(n)) 
            rtn ! false
          else {
            hashmap.put(n, i)
            rtn ! true
          }
      } | getCh ==> {
        case (n, rtn) => rtn ! hashmap.get(n)
      }
    )
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
    }.fork // TODO: why bother forking?
  }
}