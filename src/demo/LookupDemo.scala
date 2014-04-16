package demo

import cscala.NS
import java.net.InetAddress

/**
 * Registers an address with the nameserver.  Performs unsuccessful and successful lookups.
 */
object LookupDemo {

  val myport = 7701;

  /*
   * Run this twice.  First instance will start a NameServer.  Second running instance will connect to the first one's nameserver.
   */
  def main(args: Array[String]): Unit = {

    val ns = NS()

    println(ns.lookupForeign("Demo")) // will print "None" on the first attempt
    ns.registerForeign("Demo", InetAddress.getByName("localhost"), 3301)
    println(ns.lookupForeign("Demo")) // will print "Some((localhost/127.0.0.1,3301))" on the first attempt

  }

}