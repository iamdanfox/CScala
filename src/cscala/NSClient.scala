package cscala

import java.net.InetAddress


object NSClient {

  // This client will attempt to look up the address of the 'DummyEntry' service  
  def main(args: Array[String]): Unit = {
    println("Client started")
    
    NS().register("DummyService", InetAddress.getByName("localhost"), 3301)
    
    println(NS().lookup("DummyService"))
  }

}