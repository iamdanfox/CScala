/*

Copyright © 2007 - 2012 Bernard Sufrin, Worcester College, Oxford University
            and 2010 - 2012 Gavin Lowe, St Catherine's College, Oxford University

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
 A communication channel whose <code>InPort.?</code> reads
 values sent down the channel by its <code>OutPort.!</code>.
 
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 @author Gavin Lowe, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/
trait  Chan [T] extends InPort[T] with OutPort[T] with Pausable
{
  // ALT IMPLEMENTATION HOOKS
  // List of (alt,branch index) pairs registered at this InPort resp OutPort
  protected var regsIn  : List[(Alt,Int)] = Nil
  protected var regsOut : List[(Alt,Int)] = Nil

  // Results returned by commit and register 
  protected val YES    = Alt.YES
  protected val NO     = Alt.NO
  protected val MAYBE  = Alt.MAYBE
  protected val CLOSED = Alt.CLOSED

  /** Alt a registers with this channel; in is true iff a is
      registering with the InPort; n is the branch index within
      a.  This is called by registerIn and registerOut in
      subclasses. 
  */
  protected def register(a:Alt, in:Boolean, n:Int) : Int = synchronized 
  {
    val result = checkRegistered(in);
    if (result==NO) 
    { // register the port
      if (in) regsIn ::= (a,n) else regsOut ::= (a,n) 
    }
    return result
  }

  /** Release a registered alt, if there is one: release a writer if
      out=true; release a reader if out=false */
      
  protected def releaseRegistered(out:Boolean) = synchronized 
  {
    while (checkRegistered(out)==MAYBE) pause
    resetPause
  }

  /** Check if any registered alt is ready to 
      <pre>
      (a) output if out=true; 
      (b) input  if out=false. 
      </pre>
  */
  protected def checkRegistered(out:Boolean) : Int = synchronized 
  {
    var maybeFlag = false                       // has any commit returned MAYBE?
    val regs = if (out) regsOut else regsIn     // alts registered at other port

    for ( (a1,n1) <- regs )
    {
      // a1 previously registered with the other port; can it commit?
      val resp = a1.commit(n1); 
      if (resp==YES) 
      { 
        if (out) 
           regsOut = regsOut filterNot (_ == (a1,n1))
        else 
           regsIn  = regsIn filterNot (_ == (a1,n1)) 
        return YES
      } 
      else if (resp==MAYBE) 
        maybeFlag = true
      else
      { // deregister a1
        assert (resp==NO)
        if (out) 
           regsOut = regsOut filterNot (_ == (a1,n1)) 
        else 
           regsIn  = regsIn filterNot (_ == (a1,n1))
      }
    }

    // All commits have returned NO or MAYBE
    if (maybeFlag) 
       return MAYBE;
    else 
       // all returned NO
       return NO; 
  }

  /** alt a deregisters */
  def deregisterIn(a: Alt, n: Int) = synchronized
  {
    regsIn = regsIn filterNot (_ == (a,n)); 
  }
  
  /** alt a deregisters */
  def deregisterOut(a: Alt, n: Int) = synchronized
  { 
    regsOut = regsOut filterNot (_ == (a,n)); 
  }
}


object Chan
{ /** 
      A <tt>Chan.Proxy</tt> is a  <tt>Chan</tt> formed from an
      <tt>InPort</tt> and an <tt>OutPort</tt> whose contract is to
      make data output to its <tt>out</tt> available to its
      <tt>in</tt>. 
      <p>
      In the following example, <tt>Buf1</tt> returns a
      channel that behaves like a buffer of
      size 1.
      <pre>
        def Buf1[T]() : Chan[T] = 
        { val in  = OneOne[T]
          val out = OneOne[T]
          proc { repeat { out!(in?) } ({out.close}||{in.close})() }.fork
          new Proxy(in, out)
        }
      </pre>
  */
  class   Proxy[T](out: OutPort[T], in: InPort[T]) 
  extends Chan[T]
  with    InPort.Proxy[T] 
  with    OutPort.Proxy[T] 
  { val   inport = in
    val   outport = out
  }
}











