package cscala
import ox.CSO._


object test1 {
  
  val tasks = OneMany[Int]
  val responses = ManyOne[Int]
  val numtasks = 10;
  
  def main(args: Array[String]): Unit = {
    
    
    println("Hello");
    
    val system = worker() || worker() || distributor() || collector()
    
    system()
    
    println ("done")
  }
  
    // listen for a task, compute, return answer, repeat
  def worker() = proc {
    repeat {
      val x:Int = tasks?
      val y = x*x
      responses!y
    }
  }
  
  def distributor() = proc {
    // distribute N tasks
    // close channels
    for (i <- 0 until numtasks) 
      tasks!i
    tasks.closeout
  }

  def collector() = proc {
    val resps = scala.collection.mutable.Set[Int]()
    
    // collect numtasks responses
    for (i<- 0 until numtasks) resps.add(responses?)
    
    // close receiver channel
    responses.closein
    
    for (i <- resps) print(i+", ")
  }
}