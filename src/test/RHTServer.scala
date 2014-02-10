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
  var servertimeout = 30 * 1000 // five minutes

  type Client[S, T] = SyncNetIO.Client[S, T]

  def time: Long = java.lang.System.currentTimeMillis

  def main(args: Array[String]) =
    {
//      val clients = OneOne[Client[RHTReq, RHTRep]]
      val table = new scala.collection.mutable.HashMap[String, Value]
      NetIO.serverPort(port, 0, true, handleclient).fork

      Console.println("Started Server (%d) with timeout %d ".format(port, servertimeout))

      // spawn a single proc to deal with this client then terminate 
      def handleclient(client: Client[RHTReq, RHTRep]) = {
        Console.println(client.socket)
        proc("Serving: %s".format(client.socket)) {
          val start = time
          serve(client ==> {
            case Set(k, v) => {
              table.update(k, Value(v, start))
              client ! Tack(start)
            }
            case Get(k) => {
              table get k match {
                case Some(v) => client ! v
                case None => client ! Nack
              }
            }
            case Del(k) => {
              table get k match {
                case Some(Value(_, t)) => { table -= k; client ! Tack(t) }
                case None => client ! Nack
              }
            }
          }
            | after(servertimeout) ==> {
              Console.println("Slow client, killing process"); stop
            })

          Console.println("closing " + client.socket)
          client ! Close
          //client.close
          Console.println("closed " + client.socket)
        }.fork;
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

