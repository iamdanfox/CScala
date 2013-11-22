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


package ox.cso

import java.net._
import ox.cso.ObjectIO._
import ox.cso.Connection._

/**
  These utilities implement cso <code>Connection</code>s that
  transmit data to and from the network.  A synchronous connection
  is one that transmits data to the network as soon as it is
  available.  More precisely, if <code>conn</code> is a synchronous
  connection, then <code>conn!v</code> transmits <code>v</code> to
  the network immediately (without buffering), and terminates as
  soon as that has been done.  It is important to understand that
  a <code>conn!v</code> in one virtual machine does not necessarily
  synchronise with the corresponding <code>conn?</code> connected
  to the corresponding port in a peer virtual machine.
*/

object NetIO
{ 
  import ox.CSO._
  
  type Serial = ox.cso.ObjectIO.Serial
  
  /** 
     Return a <code>Server[Req,Rep]</code> network connection to
     the server at the given host and port. The connection is
     synchronous if <code>sync</code> is true.
  */ 
  def clientConnection
      [Req <: Serial, Rep <: Serial]
      (host: InetAddress, port: Int, sync: Boolean) 
      : Server[Req, Rep] =  
  { val socket = new Socket(host, port)
    socket.setTcpNoDelay(sync);
    val req = OneOne[Req]
    val rep = OneOne[Rep]
    val out = socket.getOutputStream
    val in  = socket.getInputStream
    PortToStream(req, out, sync).withName("NetIO.clientConnection P2S").fork
    StreamToPort(in, rep).withName("NetIO.clientConnection S2P").fork
    new Server(socket, req, rep)
  } 
  
  /** 
     Return a <code>Server[Req,Rep]</code> network connection to
     the server at the given host and port. The connection is
     synchronous if <code>sync</code> is true.
  */
  def clientConnection[Req <: Serial, Rep <: Serial](host: String, port: Int, sync: Boolean) : Server[Req,Rep] = 
      clientConnection[Req,Rep](InetAddress.getByName(host), port, sync)
  
  /** 
     Return a synchronous <code>Server[Req,Rep]</code> network connection to
     the server at the given host and port. 
  */
  def clientConnection[Req <: Serial, Rep <: Serial](host: String, port: Int) : Server[Req,Rep] = 
      clientConnection[Req,Rep](InetAddress.getByName(host), port, true)
  
  /** 
      Set up a server at the given port with the given
      connection backlog; each connection made to the server
      invokes <code>handle</code> on a
      <code>Client[Req,Res]</code> network connection.
      These connections are synchronous if <code>sync</code>
      is true.
  */    
  def serverPort[Req <: Serial, Rep <: Serial]
      (port: Int, backlog: Int, sync: Boolean, handle: Client[Req, Rep] => Unit) : PROC =
  { val socket = new ServerSocket(port, backlog)
    serverPort(socket, sync, handle)
  }
  
  /** 
      Set up a server at the given socket; 
      each connection made to the server
      invokes <code>handle</code> on a
      <code>Client[Req,Res]</code> network connection.
      These connections are synchronous if <code>sync</code>
      is true.
  */    
  def serverPort[Req <: Serial, Rep <: Serial]
      (socket: ServerSocket, sync: Boolean, handle: Client[Req, Rep] => Unit) : PROC =
  { proc ("serverPort: "+socket)
    { repeat
      { val client = socket.accept
        client.setTcpNoDelay(sync)
        val req = OneOne[Req]
        val rep = OneOne[Rep]
        val in  = client.getInputStream
        val out = client.getOutputStream
        PortToStream(rep, out, sync).withName("NetIO.serverPort P2S").fork
        StreamToPort(in, req).withName("NetIO.serverPort S2P").fork
        handle(new Client(client, req, rep))
      }
    }
  }
  
  /** 
      Set up a server at the given port with the default backlog;
      each connection made to the server sends a synchronous
      <code>Client[Req,Res]</code> network connection down
      <code>clients</code>.
  */    
  def serverPort[Req <: Serial, Rep <: Serial](port: Int, clients: OutPort[Client[Req, Rep]]) : PROC =
      serverPort[Req, Rep](port, 0, true, (client: Client[Req, Rep]) => clients!client)

  
  /** A NetIO.Client publishes its java.net.Socket */
  class Client[Req, Rep](theSocket: Socket, req: InPort[Req], rep: OutPort[Rep]) 
  extends 
        Connection.ProxyClient[Req, Rep](req, rep)
        { def socket = theSocket }
  
  /** A NetIO.Server publishes its java.net.Socket */     
  class Server[Req, Rep](theSocket: Socket, req: OutPort[Req], rep: InPort[Rep]) 
  extends 
        Connection.ProxyServer[Req, Rep](req, rep)
        { def socket = theSocket }
      
}







