package demo
import cscala._
import java.net.InetAddress

object PerfTest {

  def main(args: Array[String]): Unit = {
    fullyLocalNS()
    udpDistributedNS()
  }

  def udpDistributedNS(): Unit = {
    println("Speed test for FullyLocalNS.lookup (10 iterations of 100 lookups)")
    val ns = new UDPDistributedNS()
    ns.registerAddr("Google DNS", InetAddress.getByName("8.8.8.8"), 53, NameServer.DEFAULT_TTL)
    
    println("begin loop")
    val t0 = System.currentTimeMillis
    for (i <- 0 until 100) {
      val lc = ns.lookupAddr("Google DNS") // doesn't attempt a connection
    }
    val t1 = System.currentTimeMillis

//    ns.terminate()

    println(t1 - t0)
  }

  // average ~ 51.8ms per 100 instantiations
  def fullyLocalNS(): Unit = {
    println("Speed test for FullyLocalNS.lookup (10 iterations of 100 lookups)")
    for (j <- 0 until 10) {
      val ns = new FullyLocalNS()
      ns.register[String, Int]("lengthcalc", NameServer.DEFAULT_TTL, ((client) => {
        val str = client?;
        client ! (str.length)
      }))

      val t0 = System.currentTimeMillis
      for (i <- 0 until 100) {
        val lc = ns.lookup[String, Int]("lengthcalc")
      }
      val t1 = System.currentTimeMillis

      ns.terminate()

      println(t1 - t0)
    }
  }
}