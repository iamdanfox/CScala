package demo

import cscala._
import java.net._

object Tests {

  /** 
   *  Intended to be run on the first JVM available.
   */
  def main(args: Array[String]): Unit = {
    
    instantiation()
    registerForeign()
    lookupForeign()
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
  }
  
  private def lookupForeign() = {
    println("4: "+ wrap(NS().lookupForeign("Test")==Some((InetAddress.getLocalHost(),100))) )
    println("5: "+ wrap(NS().lookupForeign("NonExistent")==None))
  }

  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}