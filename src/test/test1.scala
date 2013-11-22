package test
import ox.CSO._
import java.net._
import ox.cso.ObjectIO

object test1 {

  def main(args: Array[String]): Unit = {

//    println("Starting...");
//    val clients = new OneOne[NetIO.Client[Int, Int]]
//    val myServer = NetIO.serverPort[Int, Int](3301, clients);
//    val myClient = ox.cso.NetIO.clientConnection[Int, Int](InetAddress.getByName("127.0.0.1"), 3301, false)
//    myClient!(1);
//    println(clients?)
    
    // code I would like to work:
    
//    val listener:InPort[String] = XCSO.netListener[String](3301);  // we can now listen to values by calling listener?
//    
//    
//    val foreignServer =  XCSO.netChan("127.0.0.1",3301) 
//    foreignServer ! "Hello"  // sends the "Hello" message tosome foreign server,  
//    
//    
//    println(listener?) // receives "Hello"
//    
    
    
  }
  type Serial = ox.cso.ObjectIO.Serial

  def NetListener[T <: Serial](port: Int): ?[T] = {
    val socket = new ServerSocket(port, 0)

    val receiverChan = OneOne[T]

    proc("NetListener: " + socket) {
      val client = socket.accept
      client.setTcpNoDelay(true) // enables/disables nagle's algorithm

      val in = client.getInputStream
      //val out = client.getOutputStream

      ObjectIO.StreamToPort(in, receiverChan).withName("NetListener").fork
      //        handle(new Client(client, req, rep))
    }

    return receiverChan;
  }
}