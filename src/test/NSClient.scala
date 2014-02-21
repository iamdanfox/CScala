package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import test._

object NSClient {

  // This client will attempt to look up the address of the 'DummyEntry' service

  def main(args: Array[String]): Unit = {
    println("Client started")

    // TODO: make this broadcast rather than a specific host

    // send lookup.. expecting Success(DummyEntry,localhost/127.0.0.1,0)
    var nameServer = NetIO.clientConnection[NameServerMsg, NameServerMsg]("localhost", 7700, false)
    nameServer ! Lookup("DummyEntry")
    println(nameServer?)
    nameServer.close // NameServer currently only serves one request
    
    // send broken lookup...    
//    nameServer = NetIO.clientConnection[NameServerMsg, NameServerMsg]("localhost", 7700, false)
//    nameServer ! Lookup("DoesntExist")
//    println(nameServer?)

  }

}