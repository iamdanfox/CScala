/*

Copyright © 2007 - 2012 Bernard Sufrin, Worcester College, Oxford University

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
  
  A finitely buffered Chan implementation that can be shared by both
  readers and writers.
  
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
*/
class BufImp [T: Manifest] (size: Int) extends Chan [T]
{
  assert(size>0)
  private   var buf     = new Array[T](size)
  private   val readers = new CountingSemaphore(0)
  private   val writers = new CountingSemaphore(size)
  private   var front   = 0
  private   var rear    = 0
  private   var length  = 0
  // The data is stored in buf[front..rear), wrapping round.  
  // length is number of pieces of data currently stored
  // INV: (front+length) % size = rear
  protected var name : String = _
  override  def toString = name
  
  override def close = synchronized 
  { 
      _isOpen = false 
      if (writers.forceAcquires)
         { /* waiting writers have been notified */ }
      else
      if (readers.forceAcquires) 
         { /* waiting readers have been notified */ }
  }
  
  def !(value:T) = 
  { if (!_isOpen) throw new Closed(this.toString);
    writers.acquire
    synchronized 
    { 
      if (!_isOpen) throw new Closed(this.toString)
      // store value
      buf(rear)=value; rear = (rear+1) % size; length += 1 
      // wake up reader, if there's one waiting
      releaseRegistered(false);
    }
    readers.release
  }
  
  def ? : T = 
  { if (!_isOpen && readers.count<=0) throw new Closed(this.toString)
    readers.acquire
    synchronized
    {
      if (!_isOpen && readers.isForced) throw new Closed(this.toString)
      val value = buf(front)
      front = (front+1) % size; length -= 1;
      // wake up writer, if there's one waiting
      releaseRegistered(true);
      writers.release
      value
    }
  }
  
  def ?[U] (f: T => U) : U = 
  { if (!_isOpen && readers.count<=0) throw new Closed(this.toString)
    readers.acquire
    synchronized 
    {
      if (!_isOpen && readers.isForced) throw new Closed(this.toString)
      val value = buf(front)
      front = (front+1) % size; length -= 1;
      val result = f(value)
      // wake up writer, if there's one waiting
      releaseRegistered(true);
      writers.release
      result
    }
  }

  // Stuff involving alts
  // Implementations build on register in trait Chan
  def registerIn(a:Alt, n:Int) : Int = synchronized{
    if (!_isOpen) return CLOSED; 
    if (length>0) return YES
    else register(a,true,n); 
  }
   
  def registerOut(a:Alt,n:Int) : Int =  synchronized{
    if (!_isOpen) return CLOSED; 
    if (length<size) return YES
    else register(a,false,n); 
  }

}

object BufImp extends NameGenerator("BufImp")


