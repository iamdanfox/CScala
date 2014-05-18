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
