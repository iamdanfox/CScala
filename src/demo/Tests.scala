package demo

import cscala._
import java.net._
import ox.cso.Components

object Tests {

  /** 
   *  Intended to be run on the first JVM available.
   */
  def main(args: Array[String]): Unit = {
    
    registerForeign()
    lookupForeign()
    testLookupAndConnect();
    
    println("---")
    println("done. Now run Tests2.scala")
  }
  
  private def registerForeign() ={
    println("3: "+ wrap(NS.register("Test", (InetAddress.getLocalHost(), 100), NameServer.DEFAULT_TTL)) )
//    println("   "+ wrap(NS().register("Test2", 101)) )
  }
  
  private def lookupForeign() = {
    println("4: "+ wrap(NS.lookup("Test")==Some((InetAddress.getLocalHost(),100))) )
    println("5: "+ wrap(NS.lookup("NonExistent")==None))
  }

  private def testLookupAndConnect() ={
    EchoService.startEchoService(); // starts EchoService listening on port 3302
//    NS().registerForeign("EchoService", InetAddress.getByName("localhost"), EchoService.port, NameServer.DEFAULT_TTL)
    NS.register("EchoService", (NS.nameServerAddress, EchoService.port), NameServer.DEFAULT_TTL)
    
    // pretend to be a client
    NS.connect[String,String]("EchoService") match {
      case Some(server) => {
        server ! "Hello"
        val response:String = (server?)
        println("6: "+ wrap(response == "Hello"))
      }
      case None => println("6: failed badly")
    }
  }
 
  
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}