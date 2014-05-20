package demo

import cscala._
import java.net._
import ox.cso.Components

object Tests {

  /** 
   *  Intended to be run on the first JVM available.
   */
  def main(args: Array[String]): Unit = {
    
    instantiation()
    registerForeign()
    lookupForeign()
    testLookupAndConnect();
    
//    printlnsSingleUDPDistributedNS() 
//      testMulticastSimulator()
//    testUDPDistributedNS()
//    testSharing()
    
    println("---")
    println("done. Now run Tests2.scala")
  }
  
  private def instantiation() = {
    println("1: "+ wrap(!NS.localRunning()) )
    // expecting: NS() starting a new local NameServer
    NS() 
    println("2: "+ wrap(NS.localRunning()) )
    // expecting: NS() Already running locally
    NS()
  }
  
  private def registerForeign() ={
    println("3: "+ wrap(NS().registerAddr("Test", InetAddress.getLocalHost(), 100, NameServer.DEFAULT_TTL)) )
//    println("   "+ wrap(NS().register("Test2", 101)) )
  }
  
  private def lookupForeign() = {
    println("4: "+ wrap(NS().lookupAddr("Test")==Some((InetAddress.getLocalHost(),100))) )
    println("5: "+ wrap(NS().lookupAddr("NonExistent")==None))
  }

  private def testLookupAndConnect() ={
    EchoService.startEchoService(); // starts EchoService listening on port 3302
//    NS().registerForeign("EchoService", InetAddress.getByName("localhost"), EchoService.port, NameServer.DEFAULT_TTL)
    NS().registerAddr("EchoService", NS().nameServerAddress, EchoService.port)
    
    // pretend to be a client
    NS().lookup("EchoService") match {
      case Some(server:ox.cso.InPort[String] with ox.cso.OutPort[String]) => {
        server ! "Hello"
        val response:String = (server?)
        println("6: "+ wrap(response == "Hello"))
      }
      case None => println("6: failed badly")
    }
  }
  
  
  
  private def testSharing(){
    val sim = new MulticastSimulator()
    val ns1 = new MockUDPDistributedNS(sim, "1")
    val ns2 = new MockUDPDistributedNS(sim, "2")
    ns1.registerAddr("dummy", ns1.nameServerAddress, 8888, NameServer.DEFAULT_TTL)
    Thread.sleep(100)
    println("14: "+wrap(ns2.lookupAddr("dummy")==Some((ns1.nameServerAddress, 8888))))
  }
  
  
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}