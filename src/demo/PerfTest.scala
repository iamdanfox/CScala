package demo
import cscala._
import java.net.InetAddress

object PerfTest {

  def main(args: Array[String]): Unit = {
    //    fullyLocalNS()
//    udpDistributedNS()
          registryTest()
    //    hashmapBaseline()
  }

  def hashmapBaseline(): Unit = {
    println("Speed test for hashmap lookups (10 iterations of 100 lookups)")
    for (j <- 0 until 10) {
      val h = new scala.collection.mutable.HashMap[String, InetAddress]()
      h.put("dummy", InetAddress.getLocalHost())
      val t0 = System.currentTimeMillis
      for (i <- 0 until 100) {
        val lc = h.get("dummy")
        print(lc)
      }
      println()
      val t1 = System.currentTimeMillis

      println(t1 - t0)
    }
  }

  def registryTest(): Unit = {
    println("Speed test for Registry.lookup (10 iterations of 100 lookups)")
    for (j <- 0 until 10) {
      val registry = new Registry[InetAddress]()

      val r = ox.CSO.OneOne[Boolean]
      registry.put ! ("dummy", (InetAddress.getByName("8.8.8.8"), System.currentTimeMillis, NameServer.DEFAULT_TTL), r)
      (r?)

      println("begin loop")
      val t0 = System.currentTimeMillis
      for (i <- 0 until 100) {
        val r = ox.CSO.OneOne[Option[registry.Record]]
        registry.get ! (("dummy", r))
        print(r?)
      }
      val t1 = System.currentTimeMillis

      registry.terminate!()
      println("\n")
      println(t1 - t0)
    }
  }

  // average ~ 22.6ms per 100 instantiations
  def udpDistributedNS(): Unit = {
    println("Speed test for FullyLocalNS.lookup (10 iterations of 100 lookups)")
    for (j <- 0 until 10) {
      val sim = new MulticastSimulator()
      val ns = new MockUDPDistributedNS(sim, "ns")
      ns.registerAddr("Google DNS", InetAddress.getByName("8.8.8.8"), 53, NameServer.DEFAULT_TTL)

      println("begin loop")
      val t0 = System.currentTimeMillis
      for (i <- 0 until 100) {
        val lc = ns.lookupAddr("Google DNS") // doesn't attempt a connection
        print(lc)
      }
      val t1 = System.currentTimeMillis

      //    ns.terminate()
      println("\n")
      println(t1 - t0)
    }
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