package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import java.net._
import test._

object NSClient {

  // This client will attempt to look up the address of the 'DummyEntry' service
  def nameServerLookup(name : NameServer.Name) : Option[(InetAddress,Int)] = {
    val nameServer = NetIO.clientConnection[NameServerMsg, NameServerMsg]("localhost", 7700, false) // TODO make this broadcast
    val response = nameServer!?Lookup(name)
    nameServer.close  // NameServer currently only serves one request
    return (response match {
      case Success(n,addr,port) => Some((addr,port))
      case _ => None
    })
  }
  
  def main(args: Array[String]): Unit = {
    println("Client started")

    // TODO: make this broadcast rather than a specific host
    println(nameServerLookup("DummyService"))
    
    // send broken lookup...    
//    nameServer = NetIO.clientConnection[NameServerMsg, NameServerMsg]("localhost", 7700, false)
//    nameServer ! Lookup("DoesntExist")
//    println(nameServer?)

  }

}