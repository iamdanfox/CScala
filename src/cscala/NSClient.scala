package cscala
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._
import java.net._
import test._


object NSClient {

  // This client will attempt to look up the address of the 'DummyEntry' service  
  def main(args: Array[String]): Unit = {
    println("Client started")
    
    NS().register("DummyService", InetAddress.getByName("localhost"), 3301)
    
    println(NS().lookup("DummyService"))
  }

}