package demo

import cscala._
import java.net.InetAddress
import ox.CSO._

object TestRegistry {

  def main(args: Array[String]): Unit = {
    
    val r = new Registry[Int]()
    
    
    print("1: ") // check an inserted number is retrieved correctly
    val ret = OneOne[Boolean]
    r.put!("dummy", (999, System.currentTimeMillis(), 400), ret)
    (ret?)
    val ret1 = OneOne[Option[r.Record]]
    r.get ! ("dummy",ret1)
    ret1? match {
      case Some(record) => println(wrap(record._1 == 999))
      case None => println("fail")
    }
    print("2: ") // check TTLs cause correct expiry
    Thread.sleep(500) 
    val ret2 = OneOne[Option[r.Record]] 
    r.get!(("dummy",ret2))
    println( wrap( (ret2?) == None ) )
    
    print("3: ") // check a TTL can be updated to zero (to remove a record from the registry)
    val ret3 = OneOne[Boolean] 
    r.put!("toBeKilled", (888, System.currentTimeMillis(), 100000), ret3)
    ret3?;
    val ret4 = OneOne[Boolean] 
    r.put!("toBeKilled", (888, System.currentTimeMillis()+1, 0), ret4) // +1 just to be sure!
    ret4?;
    val ret5 = OneOne[Option[r.Record]]
    r.get!(("toBeKilled",ret5))
    println( wrap( (ret5?) == None ) )
    
    print("4: ") // check registry thread can be stopped
    r.terminate!() 
    println("pass")
    
    println("---\nDone")
  }
  
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}