package cscala

import java.net.InetAddress

/**
 * Wrapper class that provides a unified interface to a non-local NameServer
 */
class ForeignNSWrapper(conn: ox.cso.NetIO.Server[Msg, Msg]) extends NameServer {

  def registerForeign(name: String, address: InetAddress, port: Int, ttl: Long): Boolean = {
    conn ! Register(name, address, port, ttl)
    val resp: Msg = (conn?)
    conn.close
    resp match {
      case Success(n, a, p) => return true
      case Failure(n) => return false
    }
  }

  def lookupForeign(name: String): Option[(InetAddress, Int)] = {
    if (!conn.isOpen()) println("conn not open") // TODO this isn't firing
    conn ! Lookup(name)
    val resp: Msg = (conn?)
    conn.close
    resp match {
      case Success(n, a, p) => return Some(a, p)
      case Failure(n) => return None
    }
  }
}