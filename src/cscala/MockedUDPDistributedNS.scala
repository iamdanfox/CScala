package cscala
import ox.CSO._

/**
 * A version of the UDPDistributedNS that doesn't actually attempt to communicate over
 */
class MockedUDPDistributedNS(simulator:MulticastSimulator, debugname:String) extends UDPDistributedNS(debugname) {

  val index = simulator.join()
  
  protected override def sendMulticast = simulator.sendMessage 
  protected override def recvMulticast = simulator.memberChans(index)

  protected override def wireUpPortsToSocket() {} // overrides all the UDP connection stuff
}

/**
 * A very naive simulation of the UDP layer.
 * Essentially just multiplexes anything sent over the `sendMessage` channel, to all members in the pool.
 * 
 * USAGE: First join the group with `.join()` then listen for messages on the `.memberChans(index)` channel
 * 
 * In-order delivery, doesn't drop packets, doesn't support leaving. Currently unstoppable. 
 */
class MulticastSimulator() {
  
  val MAX_POOLSIZE = 20;
  val MAX_MSG_BUFFER = 10;
  
  val sendMessage = ManyOne[UDPDistributedNS.UDPMessage]
  val memberChans = Buf[UDPDistributedNS.UDPMessage](MAX_MSG_BUFFER, MAX_POOLSIZE);
  
  var poolSize = 0; // the number of listeners in the pool.
  
  /**
   * Join the pool, returns an id
   */
  def join() : Int = {
    poolSize = poolSize + 1;
//    println(poolSize)
    return poolSize-1;
  }

  private def multiplexer = proc {
    repeat {
      val v = sendMessage?;
//      println("Multiplexing message: "+v)
      memberChans.foreach(c => c ! v);
    }
  }
  
  multiplexer.fork
}