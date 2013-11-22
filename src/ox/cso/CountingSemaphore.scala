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
A <tt>CountingSemaphore(permits_)</tt> is primed with <tt>permits_</tt> virtual tokens.

A call of <code>acquire</code> removes a token from the semaphore, providing
there is at least one left; otherwise it suspends the process that
invoked <code>acquire</code> until a token is available. 

A call of <code>release</code> releases a token. 
A call of <code>forceAcquires</code> releases all waiting <code>acquire</code>s,
and sets the <code>forced</code> flag. A semaphore can be forced at most once.
 
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/
class CountingSemaphore(permits_ : Int)
{ /** INVARIANT: <code>permits&le;permits_</code> */
  protected var permits = permits_ 
  /** The number of threads waiting on this semaphore for another thread to 
   release a permit */
  protected var waiting = 0
  /** This semaphore has been forced */
  protected var forced  = false
  
  /** Suspend until <code>permits&gt;0</code> */
  def acquire () : Unit =
      synchronized { 
        if (forced) throw new IllegalStateException
        while (permits==0) 
        { waiting += 1 
          wait()
          waiting -= 1
          if (forced) return
        }
        permits=permits-1
        ()
      }
  
  /** PRECONDITION: <code>permits&lt;permits_</code> */
  def release () : Unit = 
      synchronized { 
        if (forced) throw new IllegalStateException
        permits=permits+1; notify()
      }
  
  /** RETURN: the number of tokens currently available */
  def count      = synchronized { permits }
  
  /** RETURN: true if the semaphore is waiting */
  def isWaiting  = synchronized { waiting>0 }
  
  /** Set the <code>forced</code> flag; then allow all waiting
      <code>acquire</code>s to return; then return true iff there were any
      waiting.
  */
  def forceAcquires  = 
      synchronized { 
        val wereWaiting = waiting>0 // were any processes waiting?
        if (wereWaiting) { forced = true; notifyAll }
        wereWaiting 
      }
  
  /** RETURN: true if the semaphore was forced */
  def isForced = forced
}









