package cscala
import ox.CSO._

/**
 * A version of the UDPDistributedNS that doesn't actually attempt to communicate over
 */
class MockedUDPDistributedNS(simulator:MulticastSimulator) extends UDPDistributedNS {

//  val sendMulticast = OneOne[UDPDistributedNS.UDPMessage]
//  val recvMulticast = OneOne[UDPDistributedNS.UDPMessage]

  private def wireUpPortsToSocket() {}
  
//  wireUpPortsToSocket()
}

class MulticastSimulator(poolSize : Int) {
  
  def sendMessage = ManyOne[UDPDistributedNS.UDPMessage]
  def memberChans = Buf[UDPDistributedNS.UDPMessage](10, poolSize);

  private def multiplexer = proc {
    repeat {
      val v = sendMessage?;
      memberChans.foreach(c => c ! v);
    }
  }
  
  multiplexer.fork
}