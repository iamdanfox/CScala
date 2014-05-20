package demo

import ox.CSO._
import ox.cso.NetIO
import ox.cso.NetIO._
import cscala._
import cscala.NameServer._

object AkkaComparison {

  case class Greeting(who: String)
  
  // on first machine
  def handleClient(client: Client[Greeting, String]) = proc {
    client ?! (g => "Hello " + g.who) 
  }
  
  NetIO.serverPort(2552, 0, false, handleClient).fork

  NS().registerAddr("greeter", NS().nameServerAddress, 2552, TEN_MIN_TTL)

  // on second machine
  val greeter = NS().lookup[Greeting, String]("greeter") match {
    case Some(g) => g !? Greeting("Sonny Rollins")
    case None => /* error handling */
  }

}