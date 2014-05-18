package cscala
import java.net.InetAddress

/**
 * Messages sent between NameServers of TCP
 */ 
trait InterNSMsg {}

case class Register(name: String, address: InetAddress, port: NameServer.Port, timestamp: Registry.Timestamp, ttl: NameServer.TTL) extends InterNSMsg
case class Lookup(name: String) extends InterNSMsg

// these are sent as replies 
case class Success(name: String, address: InetAddress, port: NameServer.Port) extends InterNSMsg
case class Failure(name: String) extends InterNSMsg