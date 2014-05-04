package cscala

import java.net.InetAddress

/**
 * Wrapper class that provides a unified interface to a non-local NameServer
 */
class ForeignNSWrapper(conn: ox.cso.NetIO.Server[InterNSMsg, InterNSMsg]) extends NameServer {

  def registerForeign(name: String, address: InetAddress, port: Int, ttl: Long): Boolean = {
    val timestamp = System.currentTimeMillis()
    conn ! Register(name, address, port, timestamp, ttl)
    val resp: InterNSMsg = (conn?)
    conn.close
    resp match {
      case Success(n, a, p) => return true
      case Failure(n) => return false
    }
  }

  def lookupForeign(name: String): Option[(InetAddress, Int)] = {
    if (!conn.isOpen()) println("conn not open") // TODO this isn't firing
    conn ! Lookup(name)
    val resp: InterNSMsg = (conn?)
    conn.close
    resp match {
      case Success(n, a, p) => return Some(a, p)
      case Failure(n) => return None
    }
  }
}