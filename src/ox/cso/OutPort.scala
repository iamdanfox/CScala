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


package ox.cso;

/** 

{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 640 $ 
 $Date: 2013-09-24 12:48:42 +0100 (Tue, 24 Sep 2013) $
}}}

    An <code>OutPort[T]</code> is one of the endpoints of a channel that transmits
    values of type <code>T</code>.
    
    If <code>T &lt;: T'</code> then <code>OutPort[T'] &lt;:
    OutPort[T]</code> -- so we can only ever send subtypes of
    <code>T</code> down a channel with an <code>OutPort[T]</code>,
    no matter how we (type consistently) alias its output port.
    
    If <code>p</code> is an <code>OutPort[T]</code>, then
    <pre><code>p -!-&gt; { command }</code></pre> denotes
    an <code>OutPort.Event</code> that can be used
    in an <b>alt</b>; the <code>command</code> must
    normally output to <code>p</code>. 
    
    Likewise, if <code>guard</code> 
    is a Boolean expression, then 
    <pre><code>( guard &amp;&amp;&amp; p ) -!-&gt; { command }</code></pre>
    is a guarded output event that is enabled when <code>guard</code>
    is true and <code>p</code> is open. 
    
    Such events are ready to 
    fire within an <b>alt</b> when outputting to <code>p</code> would 
    not cause the currently-running process to have to wait. 
    
    <b>Design Rules:</b> (see the <b>[Restrictions]</b> section
    of <b>Alt</b>)
    
*/
trait OutPort [-T]
{  /** Output <code>value</code> to the channel */
   def ! (value: T)
   
   /** Signal that no further values will ever be transmitted via the channel */
   def close: Unit
   
     
  /** Signal that no further values will ever be written to the channel */
  def closeout : Unit = close
  
  /*  Return true iff a peer process has already committed to reading
      from the peer InPort of this OutPort.  If no commitment has been
      made and <tt>whenWriteable</tt> is non-null then it will be
      invoked when next a commitment is made. (Used only by the
      implementation of <code>Alt</code>.)
  */

  // private [cso] def isWriteable ( whenWriteable: () => Unit ) : Boolean
  
  protected var _isOpenForWrite   = true
  /** Return false iff a ! could never again succeed */
  def openForWrite: Boolean = synchronized { _isOpenForWrite } 
  /** Return false iff a ! could never again succeed */
  def isOpenForWrite(): Boolean = synchronized { _isOpenForWrite } 
  
  
  /** Return false iff a read() could never again succeed */
  private def isPortOpenForWrite() = openForWrite

  
  /** Return an unconditional event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      command is invoked; it <i>must</i> write to this port.
  */
  def -!-> (cmd: => Unit) = 
    new OutPort.OutPortEvent[T](this, ()=>cmd, isOpenForWrite)

  /** Register an attempt to use the port by an Alt: unsupported by default */
  private [cso] def registerOut(a:Alt,n:Int) : Int =  
          throw new  UnsupportedOperationException()

  /** Deregister an attempt to use the port by an Alt: unsupported by default */
  private [cso] def deregisterOut(a:Alt, n:Int): Unit =
          throw new  UnsupportedOperationException()

  // def isRegisteredOut(a:Alt, n:Int) : Boolean
}

/** OutPort object */

object OutPort{ 
  /** An <code>OutPort.Proxy[T]</code> forwards <code>OutPort[T]</code>
      methods to the <code>OutPort[T]</code> that is the value defined
      for <code>outport</code>.
  */
  trait Proxy[-T] extends OutPort[T] { 
    val outport: OutPort[T]
    override def ! (value: T) = outport ! value
    override def close        = outport.close
    // Operations for alts are forwarded
    override def registerOut(a:Alt,n:Int) : Int = 
             outport.registerOut(a, n)    // was throw new UnsupportedOperationException() 
    override def deregisterOut(a:Alt, n:Int) = 
             outport.deregisterOut(a, n) // was throw new UnsupportedOperationException() 
  }
  
  import Alt.Event

  /** A OutPortEvent is eligible when the port is openForWrite, and is
      fireable when the port is readable.  */
  case class OutPortEvent[-T](port: OutPort[T], override val cmd: ()=>Unit,
                               override val guard: ()=>Boolean) 
       extends Event(cmd, guard)
  { 
    
    def register(a:Alt,n:Int) : Int = port.registerOut(a,n:Int);         

    def deregister(a:Alt, n:Int) = port.deregisterOut(a, n:Int);

  }  
  
  /** A GuardedOutPortEvent is the syntactic precursor of a OutPortEvent */
  case class GuardedOutPortEvent[-T](port: OutPort[T], guard: ()=>Boolean){ 
    /** Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
        If the event is fired by an <code>Alt</code>, the given
        command is invoked; it <i>must</i> read from this inport.
        (Syntactic sugar for <tt>-!-&gt;</tt>)
    */    
    def --> (cmd:    => Unit) = new OutPortEvent[T](port, ()=>cmd, guard)
    
    /** Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
            If the event is fired by an <code>Alt</code>, the given
            command is invoked; it <i>must</i> write to this port.
    */    
    def -!-> (cmd:    => Unit) = new OutPortEvent[T](port, ()=>cmd, guard)
  }
    
}













