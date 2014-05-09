package cscala
import ox.CSO._

/**
 * A version of the UDPDistributedNS that doesn't actually attempt to communicate over
 */
class MockedUDPDistributedNS(simulator:MulticastSimulator, index:Int) extends UDPDistributedNS {

  protected override def sendMulticast = simulator.sendMessage // TODO: `override` keyword not working here.
  protected override def recvMulticast = simulator.memberChans(index)

  private def wireUpPortsToSocket() {}
  
//  wireUpPortsToSocket()
}

/**
 * A naive simulation of the UDP layer.
 * Essentially just multiplexes anything sent over the `sendMessage` channel, to all members in the pool.
 * 
 * In order delivery, doesn't drop packets, doesn't support leaving.
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
    return poolSize-1;
  }
  
  def receiveChan(id : Int) : ?[UDPDistributedNS.UDPMessage] = {
    assert(id < poolSize)
    memberChans(id)
  }

  private def multiplexer = proc {
    repeat {
      val v = sendMessage?;
      memberChans.foreach(c => c ! v);
    }
  }
  
  multiplexer.fork
}