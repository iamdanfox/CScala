package demo

import scala.collection.mutable.HashMap

import ox.CSO.after
import ox.CSO.proc
import ox.CSO.serve
import ox.CSO.stop
import ox.cso.NetIO
import ox.cso.SyncNetIO

/**
 * Remote hash-table client and server
 */
object RHTServer {
  var port = 8088
  var servertimeout = 5*60 * 1000 // five minutes

  type Client[S, T] = SyncNetIO.Client[S, T]

  def time: Long = java.lang.System.currentTimeMillis

  def main(args: Array[String]) =
    {
      // potential race condition here
      val store = new HashMap[String, HashMap[String,Value]]  // store :: client -> key -> value
      
      // bind a handler function to the port with specified backlog
      NetIO.serverPort(port, 0, false, handleclient).fork

      Console.println("Started Server (%d) with timeout %d ".format(port, servertimeout))

      // spawn a proc to deal with a single client then return 
      def handleclient(client: Client[RHTReq, RHTRep]) = {
        Console.println(client.socket)
        proc("Serving: %s".format(client.socket)) {
          val start = time
          
          // identify client and create private store
          val clientName = (client?).asInstanceOf[Identify].name;
          val table = store.get(clientName) match {
            case Some(t) =>  t 
            case None => 
              val t = new HashMap[String, Value];
              store.put(clientName, t);
              t 
          }
          
          // respond to client Requests or timeout
          serve(client ==> {
            case Identify(n) => {
              // client is re-identifying.
              client ! Nack;
            }
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

trait RHTRep {}
case object Ack extends RHTRep {}
case object Nack extends RHTRep {}
case class Tack(time: Long) extends RHTRep {}
case class Value(value: String, time: Long) extends RHTRep {}
case object Close extends RHTRep {}


trait RHTReq {}
case class Identify(name : String) extends RHTReq {}
case class Set(key: String, value: String) extends RHTReq {}
case class Get(key: String) extends RHTReq {}
case class Del(key: String) extends RHTReq {}
case object Timeout extends RHTReq {}

