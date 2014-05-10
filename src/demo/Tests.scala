package demo

import cscala._
import java.net._
import ox.cso.Components

object Tests {

  /** 
   *  Intended to be run on the first JVM available.
   */
  def main(args: Array[String]): Unit = {
    
//    instantiation()
//    registerForeign()
//    lookupForeign()
//    testLookupAndConnect();
//    
//    testTTLExpiry()
//    
//    testRegistryStopping()
    
    testUDPDistributedNS()
    
    println("---")
    println("done. Now run Tests2.scala")
  }
  
  private def instantiation() = {
    println("1: "+ wrap(!NS.localRunning()) )
    // expecting: NS() starting a new local NameServer
    print("    ")
    NS() 
    println("2: "+ wrap(NS.localRunning()) )
    // expecting: NS() Already running locally
    NS()
  }
  
  private def registerForeign() ={
    println("3: "+ wrap(NS().registerForeign("Test", InetAddress.getLocalHost(), 100, NameServer.DEFAULT_TTL)) )
    println("   "+ wrap(NS().register("Test2", 101)) )
  }
  
  private def lookupForeign() = {
    println("4: "+ wrap(NS().lookupForeign("Test")==Some((InetAddress.getLocalHost(),100))) )
    println("5: "+ wrap(NS().lookupForeign("NonExistent")==None))
  }

  private def testLookupAndConnect() ={
    EchoService.startEchoService(); // starts EchoService listening on port 3302
//    NS().registerForeign("EchoService", InetAddress.getByName("localhost"), EchoService.port, NameServer.DEFAULT_TTL)
    NS().register("EchoService", EchoService.port)
    
    // pretend to be a client
    NS().lookupAndConnect("EchoService") match {
      case Some(server:ox.cso.InPort[String] with ox.cso.OutPort[String]) => {
        server ! "Hello"
        val response:String = (server?)
        println("6: "+ wrap(response == "Hello"))
      }
      case None => println("6: failed badly")
    }
  }
  
  private def testTTLExpiry(){
    val l =  InetAddress.getByName("localhost")
    NS().registerForeign("ExpireIn0.5Seconds", l, 999, 500);
    println("7: "+wrap(NS().lookupForeign("ExpireIn0.5Seconds")==Some(l,999)) +"...")
    Thread.sleep(500)
    println("8: "+wrap(NS().lookupForeign("ExpireIn0.5Seconds")==None) )
    
    NS().registerForeign("DeadAlready", l, 999, NameServer.DEFAULT_TTL) 
    NS().registerForeign("DeadAlready", l, 999, 0) // Overwrite TTL to 0 to force expiry
    println("9: "+wrap(NS().lookupForeign("DeadAlready")==None))
    
    
    
  }
  
  private def testRegistryStopping(){
    val r = new Registry()
    r.terminate!()
    println("10: pass")
  }
  
  private def testUDPDistributedNS(){
    val sim = new MulticastSimulator()
    // print every UDP message to the console
    val index = sim.join()
    ox.cso.Components.console(sim.memberChans(index)).fork
    
    val ns1 = new MockedUDPDistributedNS(sim)
//    val ns2 = new MockedUDPDistributedNS(sim)
    
  }
  
  
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}