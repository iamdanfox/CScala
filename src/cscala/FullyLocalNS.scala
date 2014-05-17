package cscala

import ox.CSO._
import ox.cso.Connection._

class FullyLocalNS {
  

  def register[A, B](name: String, ttl: NameServer.TTL, handleClient: (Client[A, B]) => Unit): Boolean = {
    return false;
  }
    
  /**
   * Lookup a name in the nameserver, throw an exception otherwise.
   */
  def lookup[A,B](name: String) : Server[A,B] = {
    
  }
}