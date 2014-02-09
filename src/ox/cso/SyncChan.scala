
/*

Copyright © 2007 - 2012  Bernard Sufrin, Worcester College, Oxford University
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

package ox.cso;


/**
  
  An (Occam-style) synchronised Chan implementation that ensures that
  <code>!</code> synchronises with <code>?</code>, and that performs a
  sound (but incomplete) dynamic check for inappropriate channel-end 
  sharing.
  <p>
  If a writer overtakes a waiting writer or a reader overtakes a waiting
  reader, then an IllegalStateException is thrown, for in this case at
  least two processes must be sharing an end of the channel. This is
  something that the Scala type system can't protect from. Even worse:
  given a fast enough reader, multiple writers can go undetected (the
  dual statement is also true).
  <p>
  To <i>share</i> neither end of a synchronized channel use
  a <code>OneOne</code>.
  <p>
  To <i>share</i> the writer end of a synchronized channel use
  a <code>ManyOne</code>.
  <p>
  To <i>share</i> the reader end of a synchronized channel use
  a <code>OneMany</code>.
  <p>
  To <i>share</i> both ends of a synchronized channel use
  a <code>ManyMany</code>, but
  if (in this case) you're sure that you don't need
  synchronization, merely <i>serialization</i> 
  then use a <code>Buf</code> (which should probably have been called
  a <code>ManyManyBuf</code>). 
  
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 @author Gavin Lowe, Oxford
 $Revision: 632 $ 
 $Date: 2013-04-16 20:41:19 +0100 (Tue, 16 Apr 2013) $
}}} 
*/
class SyncChan [T] (id: String) extends Chan[T] 
{ 
  protected var obj:T = _
  protected var readerWaiting = false // a reader is waiting for a writer
  protected var writerWaiting = false // a writer is waiting for a reader
  protected var writerWaitingAltReleased = false 
    // Has an alt been fired in response to the waiting writer?
  protected var readerWaitingAltReleased = false 
    // Has an alt been fired in response to the waiting reader?
  protected var readerDone = false    // Has the reader finished?
  protected var name     =  SyncChan.genName(id) 
  override  def toString = name
  def stateToString = name + "@<" + hashCode + ">" +
                      (if (writerWaiting) "!"+obj.toString else "") + 
                      (if (readerWaiting) "?" else "") +
                      (if (readerWaiting || writerWaiting) waiter.toString else "")
  
  protected var waiter : Thread = null
  
  def this() = this(SyncChan.newName("SyncChan"))       
  
  override def close = synchronized { 
    _isOpen         = false 
    _isOpenForWrite = false 
    if (writerWaiting || readerWaiting) notify

    // Notify registered alts of closure
    for((a,n) <- regsIn)  a.chanClosed(n);
    for((a,n) <- regsOut) a.chanClosed(n);
  }
    
  def !(obj: T) = synchronized {
    val initReaderWaiting = readerWaiting;
    var resp = -1;
    if (!_isOpen) throw new Closed(name);
    readerDone = false; this.obj = obj;
    if (readerWaiting) {
      readerWaiting = false
      notify                    // notify the waiting reader      
    }
    else if (writerWaiting) 
       throw new IllegalStateException (
         this+" ! "+ obj+" : while writer "+waiter+" waiting from " + 
         Thread.currentThread()
       )
    else
    { 
      // wake up reader if there's one waiting
      releaseRegistered(false)
      // get ready to wait
      writerWaiting = true
      writerWaitingAltReleased = false
    }
    
    waiter = Thread.currentThread()
    while (!readerDone && _isOpen)  // guard against phantom notify (Nov. 2008)
          wait()                    // await the handshake from the reader
    // check if reader closed while waiting
    if (!_isOpen) throw new Closed(name)
  }

  def ? : T = synchronized {
    if (!_isOpen) throw new Closed(name)
    if (writerWaiting) 
       writerWaiting = false
    else 
    if (readerWaiting) 
       throw new IllegalStateException (
         this+" ? : while reader "+waiter+" waiting from " + 
         Thread.currentThread()
       )
    else
    { 
      // wake up writer, if there's one waiting
      releaseRegistered(true)
      // get ready to wait
      readerWaiting = true
      readerWaitingAltReleased = false 
      waiter = Thread.currentThread()
      while (readerWaiting && _isOpen) wait() // await the writer (or a close)
                                              // to guard against phantom notify (Nov. 2008)
      if (!open) throw new Closed(name) 
    }
    readerDone = true
    notify                      // handshake (the writer can proceed)
    return obj
  }
  

  def ? [U] (f: T => U) : U  = synchronized {
    if (!_isOpen) throw new Closed(name)
    if (writerWaiting) 
       writerWaiting = false
    else 
    if (readerWaiting) 
      throw new IllegalStateException (
        this+" ? : while reader "+waiter+" waiting from " + 
        Thread.currentThread()
      )
    else
    {
      // wake up writer, if there's one waiting
      releaseRegistered(true)
      // get ready to wait
      readerWaiting = true
      readerWaitingAltReleased = false 
      waiter = Thread.currentThread()
      while (readerWaiting && _isOpen) wait() // await the writer (or a close)
                                              // guard against phantom notify (Nov. 2008)
      if (!open) throw new Closed(name) 
    }
    readerDone = true
    val result = f(obj)                 // run the continuation before releasing the writer 
    notify                              // handshake (the writer can proceed)
    return result
  }
  
  // ALT IMPLEMENTATION HOOKS
  // Implementations build on register in trait Chan

  /** alt a registers at InPort; n gives the event number within a */
  override def registerIn(a: Alt, n: Int) : Int = synchronized {
    if (!_isOpen) return CLOSED
    if (writerWaiting && !writerWaitingAltReleased) 
    { 
      writerWaitingAltReleased = true
      return YES 
    }
    else register(a,true,n) 
  }
  
  /** alt a registers at OutPort; n gives the event number within a */
  override def registerOut(a: Alt, n: Int) : Int = synchronized {
    if (!_isOpen) return CLOSED
    if (readerWaiting && !readerWaitingAltReleased)
    {
      readerWaitingAltReleased = true
      return YES
    }
    else register(a,false,n); 
  }

}

object SyncChan extends NameGenerator("SyncChan-")



