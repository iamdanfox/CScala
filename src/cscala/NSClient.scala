package cscala

import java.net.InetAddress

object NSClient {

  // This client will attempt to look up the address of the 'DummyEntry' service  
  def main(args: Array[String]): Unit = {
    println("Client started")

    NS().register("DummyService", InetAddress.getByName("localhost"), 3301)

    println(NS().lookup("DummyService"))
/*
 * Exception in thread "main" ox.cso.Closed: OneOne-1
at ox.cso.SyncChan.$bang(SyncChan.scala:98)
at ox.cso.OutPort$Proxy$class.$bang(OutPort.scala:116)
at ox.cso.Connection$ProxyServer.$bang(Connection.scala:101)
at cscala.ForeignNSWrapper.lookup(ForeignNSWrapper.scala:22)
at cscala.NSClient$.main(NSClient.scala:14)
at cscala.NSClient.main(NSClient.scala)
 */
  }

}