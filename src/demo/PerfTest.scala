package demo
import cscala._

object PerfTest {

  def main(args: Array[String]): Unit = {

    for (j<- 0 until 10){
    val ns = new FullyLocalNS()
    ns.register[String, Int]("lengthcalc", NameServer.DEFAULT_TTL, ((client) => {
      val str = client?;
      client ! (str.length)
    }))

    val t0 = System.currentTimeMillis
    for (i <- 0 until 100) {
      val lc = ns.lookup[String,Int]("lengthcalc")
    }
    val t1 = System.currentTimeMillis
    
    ns.terminate()
    
    println(t1-t0)
    }
    // average ~ 51.8ms per 100 instantiations
  }
}