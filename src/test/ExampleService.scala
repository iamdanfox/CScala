package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import test._
import java.net._

/**
 * Attempts to register itself with the nameserver.
 */
object ExampleService {
  
  val myport = 7701;
  
  def main(args: Array[String]): Unit = {
    
    // send register (DummyEntry,localhost/127.0.0.1,0)
    var nameServer = NetIO.clientConnection[NameServerMsg, NameServerMsg]("localhost", 7700, false)
    nameServer ! Register("ExampleService", InetAddress.getByName("localhost"), myport)
    println(nameServer?)
    nameServer.close
    
    // service can now expect people to connect directly to it.
    
  }

}