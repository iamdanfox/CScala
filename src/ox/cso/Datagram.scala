package ox.cso

/*

Copyright © 2007, 2008 Bernard Sufrin, Worcester College, Oxford University

Licensed under the Artistic License, Version 2.0 (the "License"). 

You may not use this file except in compliance with the License. 

You may obtain a copy of the License at 

    http://www.opensource.org/licenses/artistic-license-2.0.php

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific
language governing permissions and limitations under the
License.

*/ 

/**
== Decription ==

An analogue of NetIO that uses Datagrams as transport. 
Connections are still bidirectional, but they are not
considered to be streamed. Messages between the
same pair of sockets may overtake each other. Messages may
also be dropped.

The Server object returned from clientConnection is used
to send a Client's ''requests'' to a server; the same Server
object is used to read the server's ''replies.''

The Client objects returned from serverConnection provide
clients' requests together with the addresses that they
came from; they accept replies that are annotated with
the addresses to which they should be sent.

The implementation is designed in such a way that the
connections can participate in `alt`s.  

The following is an extract from a very simple client that stops
when the server it is connected to sends it a zero. 
{{{
   val server = Datagram.clientConnection[int,int](length, host, port)
   val n      = 10
   repeat (n!=0)
   { server ! n
     server ? { case m => n=m }
   }
}}}
        
The following is an extract from a very simple server, that subtracts
1 from each number it is sent, and returns the result to the client
that sent it.
{{{
   val clients = Datagram.serverConnection[int,int](length, port)
   repeat  
   { client ? match
             { case (n, theClient)  => client!(n-1, theClient)  }
   }
   client.close
}}}

{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 470 $ 
 $Date: 2012-08-28 16:06:09 +0100 (Tue, 28 Aug 2012) $
}}}

=== HISTORY ===
Rewritten in late August 2012 to better handle transmission errors and socket timeouts.

At present there is almost no support for dealing cleanly with a socket timeout, or
other `IOException`. All that happens is that the CSO channels connected to the datagram
connection are closed; as is the connection itself.       
*/

object Datagram
{ import java.net.{SocketException,SocketTimeoutException,DatagramPacket,DatagramSocket,MulticastSocket,Socket,SocketAddress,InetAddress,InetSocketAddress}
  import java.io.IOException
  import ox.cso.ObjectIO.{ObjectOutput,ObjectInput,Serial}
  import ox.CSO._
  import ox.Format._
  
  type Packet  = DatagramPacket
  type Address = SocketAddress
  
  /**
  Construct an addressed datagram from (the serialized representation of) the given 'obj'.
  */
  def toPacket[T <: Serial](obj: T, to: SocketAddress) : Packet =
  { val byteStream = new java.io.ByteArrayOutputStream()
    val out = new ObjectOutput(byteStream)
    out.writeObject(obj)
    out.flush
    val bytes  = byteStream.toByteArray
    val length = bytes.length
    new DatagramPacket(bytes, 0, length, to)
  }
  
  /**
  Construct an unaddressed datagram from (the serialized representation of) the given 'obj'.
  */  
  def toPacket[T <: Serial](obj: T) : Packet =
  { val byteStream = new java.io.ByteArrayOutputStream()
    val out = new ObjectOutput(byteStream)
    out.writeObject(obj)
    out.flush
    val bytes  = byteStream.toByteArray
    val length = bytes.length
    new DatagramPacket(bytes, 0, length, null)
  }
  
  /**
  Construct a value of type `T` from a packet
  containing its serialized representation.
  */
  def fromPacket[T <: Serial](packet: Packet) : T =
  { val byteStream = new java.io.ByteArrayInputStream(packet.getData, packet.getOffset, packet.getLength)
    val in = new ObjectInput(byteStream)
    in.readObject.asInstanceOf[T]   
  }
  
  /**
  An adapter process that repeatedly transmits, as datagrams, data read from 'in' from the given 'sender' socket
  to the datagram socket at the given 'SocketAddress', 'to'.
  
  The process terminates (and runs cleanup) if there is an `IOError` on the socket, or the `in` port closes. 
  */
  def PortToSocket[T <: Serial](in: ?[T], sender: DatagramSocket, to: SocketAddress) ( cleanup: =>  Unit ) : PROC = 
  proc
  { repeat { in ? { case obj => tryIO ("P2S sender exception ") { sender.send(toPacket(obj, to)) } } }
    cleanup
  }
  
  /**  Property ox.cso.Datagram.log == true */
  var log: Boolean = System.getProperty("ox.cso.Datagram.log") == "true"
  
  /** 
    Evaluate `io`, causing a `CSO.repeat`-terminating `Stop` exception to be thrown 
    if an `IOException` occurs.  In the latter case, the message, and the reason for the exception, is
    printed on the error stream if `ox.cso.Datagram.log` is set.
  */ 
  @inline def tryIO(message: String)(io: => Unit) : Unit =
  {
      try   {  io  }
      catch { case e: IOException => { if (log) eprintf("%s %s%n", message, e.getMessage); stop } }                                  
  }
  
  /**
  Repeatedly deliver the values (of type 'T') represented as 
  datagrams (of size no larger than 'length') received from
  the given 'receiver' datagram socket to the  'out' port.
  
  The process terminates (and runs cleanup) if there is an `IOError` on the socket, or the `out` port closes. 
  */
  def SocketToPort[T <: Serial](length: Int, receiver: DatagramSocket, out: ![T]) ( cleanup: =>  Unit ): PROC = 
  proc
  { val buffer = new DatagramPacket(new Array[Byte](length), length)
    repeat 
    {  tryIO ("S2P receiver exception") { receiver.receive(buffer) }
       out ! (fromPacket(buffer))
    } 
    cleanup    
  }
  
  /**
        Repeatedly send the `value` components of (`value`, `socketaddress`) pairs
        read from `in` out as datagrams from the given `sender` datagram socket.  
        
        The process terminates (and runs cleanup) if there is an `IOError` on the socket, or the `out` port closes. 

  */
  def PacketsToSocket[T <: Serial](in: ?[(T, SocketAddress)], sender: DatagramSocket) ( cleanup: =>  Unit ): PROC = 
  proc
  { 
    repeat { in ? { case (obj, to) => tryIO ("Ps2S sender exception") { sender.send(toPacket(obj, to)) } } }
    cleanup
  }
  
  /**
        Receive the values (whose serialized  size is no larger than `length`) 
        arriving on the `receiver` datagram socket, and copy them, paired
        with their originating socket address, to the given output port 'out'.
  */
  def SocketToPackets[T <: Serial](length: Int, receiver: DatagramSocket, out: ![(T, SocketAddress)]) ( cleanup: =>  Unit ): PROC = 
  proc
  { val buffer = new DatagramPacket(new Array[Byte](length), length)
    repeat 
    {  tryIO ("S2Ps receiver exception") { receiver.receive(buffer) }
       out ! (fromPacket(buffer), buffer.getSocketAddress)
    }
    cleanup
  }
  
  /** Close the socket, and the request and reply channels */
  private def closeAll[Req, Rep](socket: DatagramSocket, req: Chan[Req], rep: Chan[Rep]): Unit = 
  { if (!socket.isClosed) { socket.close }
    req.close
    rep.close
  }  
  
  def clientConnection[Req <: Serial, Rep <: Serial]
      (length: Int, host: String, port: Int, timeout: Int)  : Server[Req,Rep] = 
      clientConnection[Req,Rep](length, InetAddress.getByName(host), port, timeout)
  
  /** 
      A client connection accepts requests and provides replies. The length
      parameter specifies the size of the incoming datagram buffer; this must be
      larger than any data that is expected to be received. 
  */
  def clientConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, host: InetAddress, port: Int, timeout: Int) 
      : Server[Req, Rep] =  
  { 
    val socket = new DatagramSocket()         
    val socketAddr = new InetSocketAddress(host, port)
    val req = OneOne[Req]("clientCOnnection.req")
    val rep = OneOne[Rep]("clientCOnnection.rep")
    if (timeout!=0) socket.setSoTimeout(timeout)
    PortToSocket(req, socket, socketAddr) { closeAll(socket, req, rep) }  . withName("Datagram.clientConnection P2S") . fork
    SocketToPort(length, socket, rep)     { closeAll(socket, req, rep) }  . withName("Datagram.clientConnection S2P") . fork
    new Server(socket, req, rep)
  } 
      
  def multicastClientConnection[Req <: Serial, Rep <: Serial](length: Int, host: String, port: Int, timeout: Int) : Server[Req,Rep] = 
      multicastClientConnection[Req,Rep](length, InetAddress.getByName(host), port, timeout)
  
  /** 
      A multicast client connection accepts requests and provides replies. The length
      parameter specifies the size of the incoming datagram buffer; this must be
      larger than any data that is expected to be received. 
  */
  def multicastClientConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, host: InetAddress, port: Int, timeout: Int) 
      : MulticastServer[Req, Rep] =  
  { 
    val socket = new MulticastSocket()         
    val socketAddr = new InetSocketAddress(host, port)
    if (timeout!=0) socket.setSoTimeout(timeout)
    val req = OneOne[Req]("multicastCientConnection.req")
    val rep = OneOne[Rep]("multicastCientConnection.rep")
    PortToSocket(req, socket, socketAddr)   { closeAll(socket, req, rep) }  . withName("Datagram.clientConnection P2S") . fork
    SocketToPort(length, socket, rep)       { closeAll(socket, req, rep) }  . withName("Datagram.clientConnection S2P") . fork
    new MulticastServer(socket, req, rep)
  } 
      
  /** 
      A server connection provides address-annotated requests, for
      which it listens at the given port and accepts address-annotated
      replies which it sends to the addresses with which they are
      annotated.  The length parameter specifies the size of the
      incoming datagram buffer; this must be larger than any data
      that is expected to be received.
  */
  def serverConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =  
  { 
    val socket = new DatagramSocket(port)         
    val req = OneOne[(Req, Address)]("serverConnection.req")
    val rep = OneOne[(Rep, Address)]("serverConnection.rep")
    if (timeout!=0) socket.setSoTimeout(timeout)
    PacketsToSocket(rep, socket)         { closeAll(socket, req, rep) }  . withName("Datagram.serverConnection P2S") . fork
    SocketToPackets(length, socket, req) { closeAll(socket, req, rep) }  . withName("Datagram.serverConnection S2P") . fork
    new Client(socket, req, rep)
  } 
  
  def multicastServerConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, group: String, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =
      multicastServerConnection(length, InetAddress.getByName(group), port, timeout)
      
  /** 
      A multicast server connection provides address-annotated
      requests, for which it listens at the given port and accepts
      address-annotated replies which it sends to the addresses
      with which they are annotated.  The length parameter specifies
      the size of the incoming datagram buffer; this must be larger
      than any data that is expected to be received.
  */
  def multicastServerConnection
      [Req <: Serial, Rep <: Serial]
      (length: Int, group: InetAddress, port: Int, timeout: Int) 
      : Client[(Req,Address), (Rep, Address)] =  
  { 
    val socket = new MulticastSocket(port)
    socket.joinGroup(group)
    if (timeout!=0) socket.setSoTimeout(timeout)

    val req = OneOne[(Req, Address)]("multicastServerConnection.req")
    val rep = OneOne[(Rep, Address)]("multicastServerConnection.rep")
    PacketsToSocket(rep, socket)         { closeAll(socket, req, rep) }  . withName("Datagram.multicastServerConnection P2S") . fork
    SocketToPackets(length, socket, req) { closeAll(socket, req, rep) }  . withName("Datagram.multicastServerConnection S2P") . fork
    new Client(socket, req, rep)
  } 
  
  /** A bidirectional multicast connection to a server */
  class MulticastServer[Req, Rep]
        (theSocket: MulticastSocket, 
         req:       ![Req], 
         rep:       ?[Rep]
        ) 
  extends 
        Server[Req, Rep](theSocket, req, rep)
        { def setScope(n: Int)                  = theSocket.setTimeToLive(n)
          def setLoopbackMode(disable: Boolean) = theSocket.setLoopbackMode(disable)
          override def toString =
                   "Datagram.MulticastClient("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }
  
  /** A Client connection that uses Datagrams as transport */
  class Client[Req, Rep](theSocket: DatagramSocket, req: InPort[Req], rep: OutPort[Rep]) 
  extends 
        Connection.ProxyClient[Req, Rep](req, rep)
        { def socket = theSocket 
          override def toString =
                   "Datagram.Client("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }
  
  /** A Server connection that uses Datagrams as transport */     
  class Server[Req, Rep](theSocket: DatagramSocket, req: OutPort[Req], rep: InPort[Rep]) 
  extends 
        Connection.ProxyServer[Req, Rep](req, rep)
        { def socket = theSocket 
          override def toString =
                   "Datagram.Server("+socket.getLocalSocketAddress+", remote: "+socket.getRemoteSocketAddress+")"
        }  
}














