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
  var trials = 10
  var size = 10
  var servertimeout = 10000
  var par = 1
  var sync = false
  //  var netcon = false
  var host = "localhost"

  type Client[S, T] = SyncNetIO.Client[S, T]

  def time: Long = java.lang.System.currentTimeMillis

  def main(args: Array[String]): Unit =
    {

      val server = NetIO.clientConnection[RHTReq, RHTRep](host, port, sync)

      val fromKbd = OneOne[String]

      keyboard(fromKbd).fork
      
      Thread.sleep(200) // makes the client terminal come up in Eclipse
      println("Client started. Try 'Set foo baz'")

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
                    case "." => fromKbd.close
                    case _ => (server ! Get(instr(0)))
                  }
                case 2 =>
                case 3 =>
                  instr(0) match {
                    case "del" => (server ! Del(instr(1)))
                    case _ => (server ! Set(instr(0), instr(1)))
                  }
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

