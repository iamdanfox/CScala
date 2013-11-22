/*

Copyright © 2007 - 2012  Bernard Sufrin, Worcester College, Oxford University

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
/**
A <code>Connection[Request,Reply]</code> links a
<code>Client[Request,Reply]</code> to a
<code>Server[Request,Reply]</code>

{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/

trait Connection[Request, Reply] 
{ def client() : Connection.Client[Request,Reply] 
  def server() : Connection.Server[Request,Reply]
}

object Connection
{ 
  /** The code of a server sees a <code>Client[Request,Reply]</code> --
      it reads requests from the client and writes replies to it. 
  */
  trait Client[Request,Reply] extends OutPort[Reply] with InPort[Request] 
  {  /** Serve a request */
     def ?!(serve: Request => Reply) = { this!(serve(this?)) } 
     
     /** Close both channels */
     override def close =
     { this.asInstanceOf[OutPort[Reply]].closeout
       this.asInstanceOf[InPort[Request]].closein
     }
  }
  
  /** The code of a client sees a <code>Server[Request,Reply]</code> --
      it writes requests to the server and replies from it.
  */
  trait Server[Request,Reply] extends OutPort[Request] with InPort[Reply]   
  { /** Send a message and receive its reply */
    def !?(req: Request) = { this!(req); this? } 
    
    /** Close both channels */
    override def close =
    { this.asInstanceOf[OutPort[Request]].closeout
      this.asInstanceOf[InPort[Reply]].closein
    }
    
  } 
   
  /** A <code>ProxyClient(req, rep)</code> is a <code>Client</code> that
      provides requests on <code>req</code> and accepts replies on
      <code>rep</code> */
  class ProxyClient[Request,Reply](req: InPort[Request], rep: OutPort[Reply]) 
  extends Client[Request,Reply] with OutPort.Proxy[Reply] 
  with InPort.Proxy[Request]  {
    val inport  = req
    val outport = rep

    /** Close both channels */
    override def close = { inport.close;  outport.close }          
    override def closeout = rep.closeout         
    override def closein  = req.closein  
  
    // Operations for alts not currently supported
    // def registerIn(a:Alt, n:Int) : Int = 
    //   throw new UnsupportedOperationException() 
    // def registerOut(a:Alt,n:Int) : Int = 
    //   throw new UnsupportedOperationException() 
    // def deregisterIn(a:Alt, n:Int) = 
    //   throw new UnsupportedOperationException() 
    // def deregisterOut(a:Alt, n:Int) = 
    //   throw new UnsupportedOperationException()      
  }
        
  /** A <code>ProxyServer(req, rep)</code> is a <code>Server</code> that
      accepts requests on <code>req</code> and provides replies on
      <code>rep</code> */
  class ProxyServer[Request,Reply](req: OutPort[Request], rep: InPort[Reply]) 
  extends Server[Request,Reply]      
  with OutPort.Proxy[Request]     
  with InPort.Proxy[Reply] {      
    val inport  = rep
    val outport = req
    
    /** Close both channels */
    override def close = { inport.close; outport.close } 
    override def closeout = req.closeout         
    override def closein  = rep.closein        

    // Operations for alts not currently supported
    // def registerIn(a:Alt, n:Int) : Int = 
    //   throw new UnsupportedOperationException() 
    // def registerOut(a:Alt,n:Int) : Int = 
    //   throw new UnsupportedOperationException() 
    // def deregisterIn(a:Alt, n:Int) = 
    //   throw new UnsupportedOperationException() 
    // def deregisterOut(a:Alt, n:Int) = 
    //   throw new UnsupportedOperationException()  
  }
        
  /** Construct a <code>Connection[Request,Reply]</code> from an
      arbitrary  pair of channels.
  */
  def fromChannels[Request,Reply] (req: Chan[Request], rep: Chan[Reply]) = 
      new Connection[Request,Reply] {
        val client = new ProxyClient[Request,Reply] (req, rep) 
        val server = new ProxyServer[Request,Reply] (req, rep)    
      }
  
  /** Construct a new  <code>Connection[Request,Reply]</code> from
      <code>OneOne</code> channels.
  */
  def OneOne[Request,Reply]  = fromChannels(new OneOne[Request](null),  new OneOne[Reply](null))
  
  /** Construct a new  array of <code>Connection[Request,Reply]</code> from
      <code>OneOne</code> channels.
  */
  def OneOneArray[Request,Reply](n: Int) =
  { val res = new Array[Connection[Request,Reply]](n)
    for (i<-0 until n) res(i) = OneOne[Request, Reply]
    res
  }
  
  /** Construct a new  <code>Connection[Request,Reply]</code> from
      a <code>ManyOne</code> request channel and a <code>OneOne</code>
      reply channel.
  */  
  def ManyOne[Request,Reply] = fromChannels(new ManyOne[Request], new OneOne[Reply](null))
  
  /** Construct a new  <code>Connection[Request,Reply]</code> from
      a <code>ManyMany</code> request  and 
      reply channels.
  */  
  def ManyMany[Request,Reply] = fromChannels(new ManyMany[Request], new ManyMany[Reply](null))
}















