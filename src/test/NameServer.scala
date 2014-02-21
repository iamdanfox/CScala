package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import scala.collection.mutable.HashMap
import java.net._

/**
 * Listen on a specific port.
 * Allow services to register
 * Allow processes to lookup services.
 */
object NameServer {
    
  val port = 7700;
  
  type Name = String;
  val putCh = ManyOne[(Name,InetAddress,OneOne[Boolean])]
  val getCh = ManyOne[(Name, OneOne[Option[InetAddress]])]
  
  def main(args: Array[String]) = {
    NetIO.serverPort(port, 0, false, handler).fork
  }
  
  /**
   * registry ensures no race conditions
   */
  private def registry() = {
    val hashmap = new scala.collection.mutable.HashMap[Name, InetAddress](); // should it also store timestamp of insertion?
    serve(
      putCh ==> {
        case (n,i,ch)  => 
            if (hashmap.contains(n)) ch!false
            else {
              hashmap.put(n,i)
              ch!true
            }
      } | getCh ==> {
        case (n,ch) => ch!hashmap.get(n)
      }
    )
    putCh.close; getCh.close;
  }
  
  private def handler(client: NetIO.Client[NameServerMsg, NameServerMsg]) = {
    proc("NameServer handler for "+client.socket){
      // react appropriately to the type of message
      client? match {
        case Register(name,addr,port) =>
          
        case Lookup(name) =>
          val respCh = OneOne[Option[InetAddress]]
          val resp = getCh!(name,respCh)
          respCh? match {
            case Some(addr) => client!Success(name,addr,0) // TODO port
            case None => client!Failure(name)
          }
      }
    }.fork
  }
}