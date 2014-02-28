package demo

import cscala.NS

/**
 * Attempts to register itself with the nameserver over the internet.
 */
object ExampleService {
  
  val myport = 7701;
  
  def main(args: Array[String]): Unit = {
    
    NS()
    
    // send register (DummyEntry,localhost/127.0.0.1,0)
//    var nameServer = NetIO.clientConnection[Msg, Msg]("localhost", 7700, false)
//    nameServer ! Register("ExampleService", InetAddress.getByName("localhost"), myport)
//    println(nameServer?)
//    nameServer.close
    
    // service can now expect people to connect directly to it.
    
  }

}