package demo

import cscala._

object Tests {

  /** 
   *  Intended to be run on the first JVM available.
   */
  def main(args: Array[String]): Unit = {
    
    println("test1: "+ (if (!NS.localRunning()) "pass" else "fail"))
    // expecting: NS() starting a new local NameServer
    NS() 
    println("test2: "+ (if (NS.localRunning()) "pass" else "fail"))
    // expecting: NS() Already running locally
    NS()
    
    
    
  }

}