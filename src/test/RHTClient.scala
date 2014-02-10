package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._

/**
 * Remote hash-table client and server
 */
object RHTClient {
  var port = RHTServer.port
  var sync = false
  var host = "localhost"

  type Client[S, T] = SyncNetIO.Client[S, T]

  def time: Long = java.lang.System.currentTimeMillis

  def main(args: Array[String]): Unit =
    {

      val server = NetIO.clientConnection[RHTReq, RHTRep](host, port, sync)

      val fromKbd = OneOne[String]

      keyboard(fromKbd).fork
      
      Thread.sleep(200) // makes the client terminal come up in Eclipse
      println("Client started. Try 'foo baz' to assign foo to baz")

      serve(fromKbd ==>
        {
          case raw =>
            {
              val line = raw.trim
              val instr = line.split("[ ]+")
              instr.length match {
                case 0 =>
                case 1 =>
                  instr(0) match {
                    case "." => fromKbd.close // doesn't close the connection
                    case _ => (server ! Get(instr(0)))
                  }
                case 2 =>
                  instr(0) match {
                    case "del" => (server ! Del(instr(1)))
                    case _ => (server ! Set(instr(0), instr(1)))
                  }
                case _ =>
                  println("Try 'foo baz'")
              }
            }
        }
        | server ==>
        {
          case Close => { Console.println("Server closed connection"); stop }
          case fromServer => Console.println(fromServer)
        })
      fromKbd.close
      server.close
      Console.println("Type <eof> to close down")
    }
}

