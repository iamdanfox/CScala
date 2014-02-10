package test
import ox.CSO._
import ox.cso.Connection
import ox.cso.NetIO
import ox.cso.SyncNetIO
import ox.cso.Components._

/**
 * Remote hash-table client and server
 */
object RHTServer {
  var port = 8088
  var trials = 10
  var size = 10
  var servertimeout = 5*60*1000 // five minutes
  var par = 1
//  var sync = false
//  var netcon = false
  var host = "localhost"

  type Client[S, T] = SyncNetIO.Client[S, T]

  def time: Long = java.lang.System.currentTimeMillis

  def main(args: Array[String]) =
    {
//      for (arg <- args)
//        if (arg.equals("-sync")) sync = true
//        else if (arg.equals("-netio")) netcon = true
//        else if (arg.matches("-s=[0-9]+")) {servertimeout = Integer.parseInt(arg.substring(3)) }
//        else if (arg.matches("-p=[0-9]+")) port = Integer.parseInt(arg.substring(3))
//        else if (arg.startsWith("-h=")) host = arg.substring(3)

//      if (netcon) Console.println("SyncNetIO is deprectaed right now: don't use -netio")

      val clients = OneOne[Client[RHTReq, RHTRep]]
      val table = new scala.collection.mutable.HashMap[String, Value]
      var n = 0
//      if (netcon)
//        SyncNetIO.serverPort(port, clients).fork
//      else
      NetIO.serverPort(port, clients).fork

      Console.println("Started Server (%d) with timeout %d ".format(port, servertimeout))

      serve {
        clients ==>
          { client =>
            Console.println(client.socket)
            proc("Serving: %s".format(client.socket)) {
              val start = time
              serve(client ==>
                {
                  case Set(k, v) =>
                    {
                      table.update(k, Value(v, start))
                      client ! Tack(start)
                    }
                  case Get(k) =>
                    {
                      table get k match {
                        case Some(v) => client ! v
                        case None => client ! Nack
                      }
                    }
                  case Del(k) =>
                    {
                      table get k match {
                        case Some(Value(_, t)) => { table -= k; client ! Tack(t) }
                        case None => client ! Nack
                      }
                    }
                }
                | after(servertimeout) ==> { Console.println("Slow client!"); stop })

              Console.println("closing " + client.socket)
              client ! Close
              //client.close
              Console.println("closed " + client.socket)
            }.fork;
          }
      }
    }
}

trait RHTReq {}
trait RHTRep {}
case object Ack extends RHTRep {}
case object Nack extends RHTRep {}
case class Tack(time: Long) extends RHTRep {}
case class Value(value: String, time: Long) extends RHTRep {}
case object Close extends RHTRep {}
case class Set(key: String, value: String) extends RHTReq {}
case class Get(key: String) extends RHTReq {}
case class Del(key: String) extends RHTReq {}
case object Timeout extends RHTReq {}

