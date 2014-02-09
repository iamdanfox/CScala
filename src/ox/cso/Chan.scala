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
 $Revision: 640 $ 
 $Date: 2013-09-24 12:48:42 +0100 (Tue, 24 Sep 2013) $
}}}
*/
trait  Chan [T] extends InPort[T] with OutPort[T] with AltRegister
{ override def deregisterIn(a: Alt, n: Int): Unit  = AltderegisterIn(a, n)
  override def deregisterOut(a: Alt, n: Int): Unit = AltderegisterOut(a, n)
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
  with    InPort[T] 
  with    OutPort[T] 
  { val   inport = in
    val   outport = out
    
    override def ! (value: T) = outport ! value
    override def close        = outport.close

    override def registerOut(a:Alt,n:Int) : Int = 
             outport.registerOut(a, n)    // was throw new UnsupportedOperationException() 
    override def deregisterOut(a:Alt, n:Int) = 
             outport.deregisterOut(a, n) // was throw new UnsupportedOperationException() 
    
    override def ?      = inport.?
    override def ?[U] (f: T=>U) = f(inport?)
    override def read   = inport.?
    override def open   = inport.open

    override def registerIn(a:Alt, n:Int) : Int = 
                 inport.registerIn(a, n)   // was throw new UnsupportedOperationException() 
    override def deregisterIn(a:Alt, n:Int) = 
                 inport.deregisterIn(a, n) // was throw new UnsupportedOperationException() 
  }
}













