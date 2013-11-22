package test
import ox.CSO._


object test1 {

  def main(args: Array[String]): Unit = {
    
    println("Hello");
    
    val myChan = OneOne[Int];
    
    (proc {  myChan ! 1 } || proc {println(myChan?);})();
    
    
  }

}