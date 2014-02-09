/*

Copyright © 2007 - 2012 Bernard Sufrin, Worcester College, Oxford University
        and 2010 - 2012 Gavin Lowe, St Catherine's College, Oxford University.

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

 An <code>InPort[T]</code> is one of the endpoints of a
 channel that transmits values of type <code>T</code>. Its
 principal method is <code>?</code> (sometimes written
 <code>read</code>.
 <p>
 If <code>p</code> is an <code>InPort[T]</code>, and <code>cont</code> 
 denotes a function of type <code>T=>Unit</code> then both the following
 expressions
 <pre>
 <code>(guard &&& p)==>cont</code>   
 <code>p==>cont</code>
 </pre>
 
 yield <code>InPort.Event</code>s for use in an
 <code>Alt</code>.
 Firing such an event results in the execution of
 the command: <code>cont(p?)</code> (the function is applied to
 the next value read from the port).</p>
 
 <p>
 Events of the form 
 <code>(guard &&& p)==>>cont</code> and
 <code>p==>>cont</code> may also be used in an <code>Alt</code>.
 
 Firing such an event results in the execution of
 the command: <code>p?cont</code> (the function is applied to
 the next value read from the port in an <i>extended rendezvous read</i>),
 <i>i.e.</i>
 the peer process output command that fires the event is
 synchronized with the <i>end</i> of the evaluation of the command.</p>
 
     <p><b>Design Rules:</b> (see the <b>[Restrictions]</b> section
    of <b>Alt</b>)<p>
 
*/


trait InPort [+T]
{ 
  /** Block until a value is available for reading, then read and return it. */
  def ? (): T 
  
  /** Block until a value <tt>t</tt> is available for
      reading, then return the result of applying <tt>f</tt> to
      it; synchronisation with the sender is at the end of
      the computation of <tt>f(t)</tt> (this is sometimes
      called an <i>extended rendezvous</i>)
  */
  def ?[U] (f: T => U): U 
  
  /** Synonym for <code>?()</code> */
  def read () : T = ?
  
  /** Signal that no further values will ever be transmitted via the channel */
  def close : Unit
  
  /** Signal that no further values will ever be read from the channel */
  def closein : Unit = close
  
  /** Return true iff a value is available now for reading.
      If no value is available and <tt>whenReadable</tt> is non-null
      then it will be invoked when next a value is available for
      reading. (Used only by the implementation of <code>Alt</code>.)
  */
  // private [cso] def isReadable ( whenReadable: () => Unit ) : Boolean
  
  protected var _isOpen   = true
  /** Return false iff a read() could never again succeed */
  def open: Boolean = synchronized { _isOpen } 
  /** Return false iff a read() could never again succeed */
  def isOpen(): Boolean = synchronized { _isOpen } 
  
  
  /** Return false iff a read() could never again succeed */
  private def isPortOpen() = open
  
  /** Return an unconditional event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      command is invoked; it <i>must</i> read from this inport.
      (Syntactic sugar for <tt>-?-&gt;</tt>)
  */
  def --> (cmd: => Unit)    = new InPort.InPortEvent[T](this, ()=>cmd, isOpen)
    
  /** Return an unconditional event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      command is invoked; it <i>must</i> read from this inport.
  */
  def -?-> (cmd: => Unit)    = new InPort.InPortEvent[T](this, ()=>cmd, isOpen)
    
  /** Return an unconditional event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      continuation function is applied to the
      next value read from this inport.
  */
  def ==> (cont: T => Unit) = new InPort.InPortEvent[T](this, (()=>cont(this?)), isOpen)
    
  /** Return an unconditional event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      continuation function is invoked in an extended rendezvous
      with the next value read from this inport.
   */  
   def ==>> (cont: T => Unit) = new InPort.InPortEvent[T](this, ()=>this ? cont, isOpen)
    
  /** 
      Prepare to return an InPort event with a nontrivial guard.
  */
  @deprecated("Use the form guard &&& port", "2012") def apply (guard: => Boolean) = 
            new InPort.GuardedInPortEvent(this, ()=>(open&&guard))

  /** Register an attempt by an Alt to use the port: unsupported by default */
  private [cso] def registerIn(a:Alt, n:Int) : Int = 
          throw new  UnsupportedOperationException()

  /** Deregister an attempt by an Alt to use the port: unsupported by default  */
  private [cso] def deregisterIn(a:Alt, n:Int) : Unit =
          throw new  UnsupportedOperationException()

  // def isRegisteredIn(a:Alt, n:Int) : Boolean
}

object InPort{ 

  import Alt.Event

  /** A InPortEvent is eligible when the port is open, and is fireable when
   the port is readable.  */
  case class InPortEvent[+T](port: InPort[T], override val cmd: ()=>Unit, 
                             override val guard: ()=>Boolean) 
       extends Event(cmd, guard)
  { 
 
    def register(a:Alt, n:Int) : Int = port.registerIn(a, n);

    def deregister(a:Alt, n:Int) = port.deregisterIn(a, n);

  }  
  
  /** A GuardedInPortEvent is the syntactic precursor of a InPortEvent */
  case class GuardedInPortEvent[+T](port: InPort[T], guard: ()=>Boolean){
    /** Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
        If the event is fired by an <code>Alt</code>, the given
        command is invoked; it <i>must</i> read from this inport.
        (Syntactic sugar for <tt>-?-&gt;</tt>)
    */    
    def --> (cmd:    => Unit) = new InPortEvent[T](port, ()=>cmd, guard)
    
    /** Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
        If the event is fired by an <code>Alt</code>, the given
        command is invoked; it <i>must</i> read from this inport.
    */    
    def -?-> (cmd:    => Unit) = new InPortEvent[T](port, ()=>cmd, guard)
        
    /** Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
        If the event is fired by an <code>Alt</code>, the given
        continuation function is applied to the
        next value read from this inport.
    */
    def ==> (cont: T => Unit) = new InPortEvent[T](port, (()=>cont(port?)), guard)
    
    /**
      Return a <i>conditional</i> event, for use in an <tt>Alt</tt>.
      If the event is fired by an <code>Alt</code>, the given
      continuation function is invoked in an extended rendezvous
      with the next value read from this inport.
    */
    def ==>> (cont: T => Unit) = new InPortEvent[T](port, ()=>port?cont, guard)
  }
  
   
  
  /** An <code>InPort.Proxy[T]</code> forwards <code>InPort[T]</code>
      methods to the <code>InPort[T]</code> that is the value defined
      for <code>inport</code>.
  */
  trait Proxy[+T] extends InPort[T]{
    val inport: InPort[T]
    override def ?      = inport.?
    override def ?[U] (f: T=>U) = f(inport?)
    override def read   = inport.?
    override def open   = inport.open
    // Operations for alts are forwarded
    override def registerIn(a:Alt, n:Int) : Int = 
                 inport.registerIn(a, n)   // was throw new UnsupportedOperationException() 
    override def deregisterIn(a:Alt, n:Int) = 
                 inport.deregisterIn(a, n) // was throw new UnsupportedOperationException() 
  } 
  
}



















