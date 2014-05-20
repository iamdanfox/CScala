package demo
import cscala._

object TestDistributed {

  def main(args: Array[String]): Unit = {
    printlnSingle()
    println("\n\n")
    testUDPDistributedNS()
    println("\n\n")
    testMulticastSimulator()
    testSharing()
  }

  private def printlnSingle(){
    val sim = new MulticastSimulator()
    // print every UDP message to the console
    val index = sim.join()
    (ox.CSO.proc { ox.CSO.repeat { Console.println("[UDP] " + (sim.memberChans(index)?)) }}).fork
    
    val ns1 = new MockUDPDistributedNS(sim, "1")
    println("[Tests] ns1 initialised")
  }
  
  /**
   * This method is intended to test that a newly started up nameserver can be bootstrapped correctly.
   */
  private def testUDPDistributedNS(){
    val sim = new MulticastSimulator()
    // print every UDP message to the console
    val index = sim.join()
    (ox.CSO.proc { ox.CSO.repeat { Console.println("[UDP] " + (sim.memberChans(index)?)) }}).fork
    
    val ns1 = new MockUDPDistributedNS(sim, "1")
    ns1.registerAddr("dummy", ns1.nameServerAddress, 8888, NameServer.DEFAULT_TTL) // TODO. why is ns1 saving this?
    
    Thread.sleep(3000)
    
    val ns2 = new MockUDPDistributedNS(sim, "2")

    println("13: " + wrap(ns2.lookupAddr("dummy")==Some((ns1.nameServerAddress, 8888))))
  }
  
  private def testMulticastSimulator(){
    val sim = new MulticastSimulator();
    val i1 = sim.join()
    sim.sendMessage!UDPDistributedNS.AnyoneAwake
    println("11: "+ wrap(UDPDistributedNS.AnyoneAwake == (sim.memberChans(i1)?)))
    val i2 = sim.join()
    sim.sendMessage!UDPDistributedNS.RequestFill(null,null)
    println("12: "+ wrap(UDPDistributedNS.RequestFill(null,null) == (sim.memberChans(i2)?)))
  }
  
  
  private def testSharing(){
    val sim = new MulticastSimulator()
    val ns1 = new MockUDPDistributedNS(sim, "1")
    val ns2 = new MockUDPDistributedNS(sim, "2")
    ns1.registerAddr("dummy", ns1.nameServerAddress, 8888, NameServer.DEFAULT_TTL)
    Thread.sleep(100)
    println("14: "+wrap(ns2.lookupAddr("dummy")==Some((ns1.nameServerAddress, 8888))))
  }
  
  
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}