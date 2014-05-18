package demo

import cscala._
import Tests.wrap
import java.net.InetAddress

object Tests2 {

  /**
   * Intended to be run on another JVM after Tests.scala has been run.
   */
  def main(args: Array[String]): Unit = {
    
    // NS() shouldn't have a local instance.
    
    println("1: "+ (if (!NS.localRunning()) "pass" else "fail"))
    // expecting: NS() trying to connect to local JVM
    NS() 
    println("2: "+ (if (!NS.localRunning()) "pass" else "fail"))
    // expecting: NS() trying to connect to local JVM
    NS()
    
    lookupForeign()
    
    println("---")
    println("all done. Remember to stop both JVMs.")
  }
  
  private def lookupForeign() = {
    // already registered in 'Tests.scala'
    println("3: "+ wrap(NS().lookupAddr("Test")==Some((InetAddress.getLocalHost(),100))) )
    println("4: "+ wrap(NS().lookupAddr("NonExistent")==None) )
  }

}