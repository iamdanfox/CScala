/* 

Copyright © Gavin Lowe, 2008 - 2012

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
import ox.CSO._

/**
<code>Barrier(n)</code> provides an object that allows <code>n</code>
processes to synchronise.  Each call to <code>sync</code> will block until
<code>n</code> processes have called it, at which point all will be unblocked.

{{{
 @version 03.20120824
 @author Gavin Lowe, Oxford
 $Revision: 552 $ 
 $Date: 2012-08-25 12:54:06 +0100 (Sat, 25 Aug 2012) $
}}}
*/

class Barrier(n:Int){
  assert(n>1);
  private var waiting = 0; // number of processes currently waiting
  private val waitSem = new Semaphore; waitSem.down
  private val mutex = new Semaphore;

  /** Try to synchronise.  Block until all <code>n</code> processes call 
   sync */ 
  def sync = {
    mutex.down;
    if(waiting==n-1){ waitSem.up; }
    else{ 
      waiting+=1; mutex.up; waitSem.down; 
      // Wait until woken
      waiting-=1; 
      if(waiting==0) mutex.up; else waitSem.up;
    }
  }
}
    
// -------------------------------------------------------

/** 
Barrier synchronisation between <code>N</code> processes, taking ''O(log N)''
time.

Each process should have an identity in [0..N), provided as an argument to
<code>sync</code>.

This implementation is based on the barrier tree synchronisation algorithm in
Section 3.4.2 of Foundations of Multithreaded, Parallel, and Distributed
Programming, by Gregory Andrews.  The processes are arranged in a heap.  Each
leaf process sends a "ready" message to its parent.  Each intermediate node
receives "ready" messages from its children, and sends a "ready" message to
its parent.  The root node receives "ready" messages from its children, and
sends a "go" message back.  The "go" messages are propogated by the reverse
routes.

{{{
$Id: Barrier.scala 552 2012-08-25 11:54:06Z sufrin $
@author Gavin Lowe, Oxford
}}}
*/

class TreeBarrier(N:Int){
  // Channels:
  private val ready = OneOne[Unit](N);
  private val go    = OneOne[Unit](N);
  // The channels are indexed by the *child's* identity; so ready is indexed
  // by the sender's identity, and go is indexed by the receiver's identity

  /** Try to synchronise.  Block until all <code>n</code> processes call 
   sync.   
   @param me the identity of the calling processes. 
  */ 
  def sync(me : Int) = {
    val child1 = 2*me+1; val child2 = 2*me+2;
    // Wait for ready signals from both children
    if(child1<N) ready(child1)?;
    if(child2<N) ready(child2)?;
    // Send ready signal to parent, and wait for go reply, unless this is the
    // root
    if (me!=0) { ready(me)!(); go(me)? }
    // Send go signals to children
    if(child1<N) go(child1)!();
    if(child2<N) go(child2)!();
  }

}

// -------------------------------------------------------

/**
Barrier synchronisation with combining of values.  If nodes pass in values ''x1,
..., x_n'', then they should all obtain ''e `f` x1 `f` x2 `f` ... `f` xn'' (f is 
expected to be associative). 

{{{
$Id: Barrier.scala 552 2012-08-25 11:54:06Z sufrin $
@author Gavin Lowe, Oxford
}}}
*/

class CombiningBarrier[T](n:Int, e:T, f:(T,T)=>T){
  assert(n>1);
  private var waiting = 0; // number of processes currently waiting
  private val waitSem = new Semaphore; waitSem.down
  private val mutex = new Semaphore;
  private var result = e;

  /** Try to synchronise.  Block until all <code>n</code> processes call sync.
   @param x value to be combined with the values provided by other processes.
   @return the result of combining all provided arguments using
   <code>f</code>. */
  def sync(x:T) : T = {
    mutex.down;
    result = f(result, x);
    if(waiting==n-1){ val res = result; waitSem.up; return res; }
    else{ 
      waiting+=1; mutex.up; waitSem.down; 
      // Wait until woken
      waiting-=1; 
      if(waiting==0){ val res = result; result = e; mutex.up; return res; } 
      else{ val res = result; waitSem.up; return res; }
    }
  }
}





