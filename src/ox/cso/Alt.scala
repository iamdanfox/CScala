/*

Copyright © 2007-2012 Bernard Sufrin, Worcester College, Oxford University
        and 2010-2012 Gavin Lowe, St Catherine's College, Oxford University

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
import ox.CSO._;



/**


== Alt Commands ==
`Alt` objects provide the core of
the implementation of CSO commands of the '''alt''' family, namely:
'''alt''', '''serve''', '''prialt''', '''priserve'''
and the `Alt` constructors can be invoked by a variety
of CSO notations. 


{{{
@version 03.20120824
@author Bernard Sufrin, Oxford
@author Gavin Lowe, Oxford
$Revision: 553 $ 
$Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}

The preferred notation is
{{{
    altcommand ( events
               | events
               | events
               ...
               | events
               )
}}}

where '''events''' is either a simple event or an event sequence constructed
using the notation
{{{              
    | (for ( variable <- collection ) yield simpleeventexpression(variable))  
}}}

In fact, an `Alt` is a function-like object constructed from a sequence of guarded
`Alt.Event`s (of which the most familiar kind are `InPort.Event`s
and `OutPort.Event`s). 

A simple `InPort.Event` usually takes one of the forms: 
{{{
   inport (guard)     ==> { bv => cmd }                     // deprecated  
   (guard &&& inport) ==> { bv => cmd } 
   inport             ==> { bv => cmd }
}}}

(in the third form the guard is implicitly `true`.)

Whenever the guard is true <i>and the port is open</i>, the
event is said to be enabled. When the port of an enabled event
is ready to read (''i.e.'' when a process at the 
other end of port's channel is prepared to cimmit
to writing), the event is said to be ready.  We <i>fire</i>
such an event by evaluating the expression 
{{{
        { bv => cmd }(inport?)
}}}
''i.e.'' we read a value from the port, then apply the function 
{{{
        { bv => cmd }
}}}
to that value.


Additional simple `InPort.Event`s take the form:
{{{
   inport (guard)     ==>> { bv => cmd }                  // deprecated   
   (guard &&& inport) ==>> { bv => cmd } 
   inport             ==>> { bv => cmd }
}}} 

(in the third form the guard is implicitly `true`.)

We ''fire''
this kind of event by evaluating the expression 
{{{
        (inport?{ bv => cmd })
}}}
''i.e.'' we perform an extended rendezvous read from the port.


== Primitive Event Notation ==
The above forms are syntactic sugar for events built using
the following more primitive notation, in which the `command`
is required to read from the `inport`.
{{{
   inport (guard)     -?-> { command }              //deprecated
   (guard &&& inport) -?-> { command } 
   inport             -?-> { command }
}}} 
(the operator `-->` is a synonym for `-?->`).
The `command` '''must''' input from the `inport`.  

== Timeouts and Failures ==
There are also two forms of ''pseudo-event'', namely:
{{{
   after(expression) ==> { cmd } 
   orelse            ==> { cmd }
}}} 
  
When `f: Alt` is applied, as `f()`, (this is done automatically
in the case of a CSO command of the '''alt''' family)
the guards are first
evaluated (in the case of an after guard, the Long-valued expression
is evaluated and determines a timeout in milliseconds). 
If any of the enabled events is ready, then one of them
is chosen (nondeterministically), and the event is fired.  
(See '''Fairness'')

If no enabled event is ready, then the current thread/process
is descheduled until one becomes ready.

If no event is enabled, or if they all become disabled while
the thread is suspended [See Note 2], then an `Abort` `Stop`
exception is thrown (unless the `Alt` has an
`orelse` event associated with it, in which case
that event's command is executed.


If a timeout event is specified with an 
{{{
    after(expression)==>{cmd}
}}}
guard, and the timeout elapses before any other
event becomes ready, then the timeout command is executed.

== OutPort Events ==
Although they do not appear in classical '''occam''', CSO also implements
output port events which can also participate in `Alt`s.
{{{
   outport (guard)     -!-> { command }              //deprecated
   (guard &&& outport) -!-> { command } 
   outport             -!-> { command }
}}}
Such events are ready when a process 
is committed to read from the other end of the
`outport`'s channel.  The `{command}` '''must'''
output to the `outport`.

See the documentation of `OutPort` for further details.

== Restrictions ==

  -   Guards must be side-effect free; as must the timeout 
      specification expression in an `after` event.
  
  -   No  more than one `after` event may appear.
  
  -   No  more than one `orelse` event may appear.
  
  
  -   If a shared port (e.g. of a ManyMany channel, whose ports can
      be shared by several senders and by several receivers)
      is involved in an alt, it must not simultaneously be read
      or written by a non-alt process;  

  -   An alt may not have two simultaneously enabled branches
      using the same channel (although it may have two branches
      using the same channel with disjoint guards);
        


== Fairness ==
This implementation is ''fair'' inasmuch as
successive applications of the same `Alt` object in
which the same collection of events turn out to be ready result
in distinct events being fired.
''Fairness'' is implemented in a non priority alt by starting the
scan for a ready port just after the most recently-selected port.

== Soundness of the Implementation ==
The current implementation (and a proof of its probabilistic soundness) is 
presented in ''Implementing Generalised Alt: A
Case Study in Validated Design using CSP'', Gavin Lowe.

@author Bernard Sufrin, Oxford
@author Gavin Lowe, Oxford
@version 0320120824
        
*/  




class Alt(events: Seq[Alt.Event], priAlt: Boolean)
{ 

  /** Regular constructor */
  def this(events: Seq[Alt.Event]) = this(events, false)  
  def this(syntax: Alt.Syntax) = this(syntax.elements, false)

  private val theAlt = this;
  private val eventCount = events.length;

  /* External methods */
  /** execute this `Alt` */
  def apply (): Unit = MainAlt.apply();

  /** Repeatedly execute this `Alt` until a `Stop` 
      exception of some kind is thrown. This is used to implement
      the CSO command `'''serve'''( events )`.
  */
  def repeat = ox.CSO.repeat { this() };

  /** Receive request from a channel to commit to selecting a branch.
      @param n the index of the branch.
      @return one of the values Alt.YES, Alt.NO or Alt.MAYBE
   */
  private [cso] def commit(n:Int) : Int = Facet.commit(n);

  /** Receive signal from a channel that it has closed.
      @param n the index of the branch.
  */
  private [cso] def chanClosed(n:Int) = Facet.chanClosed(n)

  /* Status values */
  private val INIT = 0;  private val PAUSE = 1; private val WAIT = 2; 
  private val WAITTO = 3; private val DEREG = 4; private val DONE = 5;
  private val COMMIT = 6; private val TIMEDOUT = 7;
 
  /* Results returned by commit and register */ 
  private val YES = Alt.YES; private val NO = Alt.NO; 
  private val MAYBE = Alt.MAYBE; private val CLOSED = Alt.CLOSED;

  // -------------------------------------------------------
  /* The Main Alt */
  private object MainAlt extends Pausable{ 

    private var waiting = false; // flag to indicate the alt is waiting
    private var toRun = -1; // branch that should be run
    private var allBranchesClosed = false; // are all branches closed?
    private var n = 0; // index of current event

    /* Execute the alt */
    def apply (): Unit = synchronized {
      Facet.changeStatus(INIT); Arbitrator.checkRace(INIT);
      
      var enabled = new Array[Boolean](eventCount); // values of guards
      var reged = new Array[Boolean](eventCount); // is event registered?
      var nReged = 0;                    // number of registered events

      var done = false; // Have we registered all ports or found a match?
      var success = false; // Have we found a match?
      var maybes = false; // have we received a MAYBE?
      if(priAlt) n=0;
      var timeoutMS : Long = 0; // delay for timeout
      var timeoutBranch = -1; // index of timeout branch
      var orElseBranch = -1; // index of orelse branch

      toRun = -1; allBranchesClosed = false;

      // Evaluate guards; this must happen before registering with channels
      for(i <- 0 until eventCount){ 
        // closed(i) = false; 
        enabled(i) = events(i).guard();
      }

      while(!done) {
        var count=0; // number of events considered so far
        while(count<eventCount && !done){
          if(!reged(n)){ // if event(n) not already registered
            val event = events(n); 
            if(enabled(n)){
              event match {
                case Alt.TimeoutEvent(tf, _) => 
                  if(timeoutBranch>=0)  
                    throw new RuntimeException(
                      "Multiple 'after(timeout)' events in alt");
                  else{ timeoutMS = tf(); timeoutBranch = n; reged(n) = true; }
                case Alt.OrElseEvent(_,_) =>
                  if(orElseBranch>=0)
                    throw new RuntimeException(
                      "Multiple 'orelse' events in alt");
                  else{ orElseBranch = n; reged(n) = true; }
                case _ => { // InPortEvent or OutPortEvent
                  event.register(theAlt,n) match{
                    case YES => {
                      Facet.changeStatus(DEREG); toRun = n; 
                      done=true; success=true;
                    }
                    case NO => { reged(n) = true; nReged += 1; } 
                    case MAYBE => maybes = true;
                    case CLOSED => // channel has just closed 
                      enabled(n) = false;
                  } // end of event.register(theAlt,n) match
                } // end of case _
              } // end of event match
            } // end of if(enabled(n))
          } // end of if(!reged(n))

          n = (n+1)%eventCount; count += 1; 
        } // end of inner while
        
        if(!done) // All registered, without finding a match
          if(maybes){ 
            // Random length pause to break symmetry
            Facet.changeStatus(PAUSE); pause;
            // see if a commit has come in 
            toRun = Facet.getToRun; 
            if(toRun<0){ // No, so reset variables for next round
              maybes = false; 
            }
            else{ done = true; success = true; } // done
          } // end of if(maybes)
          else done=true;
      } // end of outer while
      resetPause;

      // All events now registered with their channels

      if(!success){ // No registration returned YES
        if(nReged==0){ // no event enabled, so behave as an OrElse *****************
          if(orElseBranch>=0) toRun = orElseBranch
          else throw new Abort; 
        }
        else
        if(timeoutMS==0){ // no timeout
          // Need to wait for a channel to become ready
          waiting=true; 
          allBranchesClosed = Facet.setReged(nReged); 
          if(!allBranchesClosed) while(waiting) wait(); // wait to be awoken
        }
        else{ // with timeout
          Facet.changeStatus(WAITTO); waiting=true;
          wait(timeoutMS); // wait to be awoken or for timeout
          if (waiting) { 
            // assume timeout was reached (this could be a spurious wakeup)
            if(Arbitrator.checkRace(TIMEDOUT)){
              waiting = false; toRun = timeoutBranch; 
            }
            else // A commit was received just before the timeout.
              if (!allBranchesClosed)   // **************** Don't wait if there's no hope
                 while(waiting) wait()  // Wait to be woken
          } // end of if(waiting)
        } // end of else (with timeout)
      } // end of if(!success)

      // Can now run branch toRun
      // Check if all branches closed
      if(allBranchesClosed){
        if(orElseBranch>=0) toRun = orElseBranch
        else throw new Abort; 
      }
      
      
      
      // Deregister events
      Facet.changeStatus(DEREG); 
      for(n <- 0 until eventCount)
        if(n != toRun && reged(n)) events(n).deregister(theAlt,n)

      // Finally, run the selected branch
      Facet.changeStatus(DONE); events(toRun).cmd(); 
    } // end of apply

    /* Implementation of WakeUp events; called by Facet to wake up the MainAlt 
    */
    def wakeUp(n:Int) = synchronized {
      assert(waiting); // use of Arbitrator should ensure this
      toRun = n; waiting = false; notify(); // wake up MainAlt
    }

    /* Receive notification from Facet that all branches have been closed */
    def allClosed = synchronized{ 
      assert(waiting); allBranchesClosed=true; waiting=false; notify(); 
    }

  } // end of MainAlt


  // -------------------------------------------------------
  /* The Facet */ 
  private object Facet {
    private var status = DONE; 
    private var toRun = -1; // stored value received in a commit during the
                            // contention-breaking phase
    private var nClosed = 0; // number of closed channels
    private var nReged = -1; // number of registered processes not closed

    /* Messages from channel to commit. */
    def commit(n:Int) : Int = synchronized{
      if(status==INIT){ return MAYBE; }
      else if(status==WAIT || status==WAITTO){ 
        if(Arbitrator.checkRace(COMMIT)){
          // wake up MainAlt
          MainAlt.wakeUp(n); status = DEREG; return YES; 
        }
        else return NO; // timeout has occurred just before the commit
      }
      else if(status==PAUSE){ 
        if(toRun<0){ toRun = n; return YES; } else return NO
      }
      else{ assert(status==DEREG); return NO; }
    }

    /* Messages from channels to close */
    def chanClosed(n:Int) = synchronized{
      if(status==INIT || status==PAUSE){  nClosed+=1; }
      else if(status==WAIT){
        nClosed+=1; 
        if(nReged==nClosed) MainAlt.allClosed; 
      }
      // else ignore, as it makes no difference
    }

    /* Command from MainAlt to change status to status s. */
    def changeStatus(s:Int) = synchronized { 
      assert(status==INIT && (s==DEREG || s==PAUSE || s==WAITTO) || 
             (status==WAIT || status==WAITTO) && (s==DEREG || s==DONE) ||
             status==DEREG && (s==DONE || s==DEREG) || 
             status==DONE && s==INIT);
      status = s; 
      if(s==INIT){ // reset variables
        nClosed = 0; nReged = 0;
      }
    }

    /* Receive the number of registered processes from MainAlt, ready for a
       wait.  Return result indicates whether all processes have been closed.
    */
    def setReged(nReged:Int) : Boolean = synchronized{
      assert(status==INIT); this.nReged = nReged; 
      status = WAIT; return(nReged==nClosed);
    }

    /* Pass to MainAlt any request that has come in while it was sleeping in
       the contention-breaking phase.
    */
    def getToRun : Int = synchronized{ 
      assert(status==PAUSE);
      if(toRun>=0) status = DEREG else status = INIT; 
      // TODO: do we need the "status = DEREG "
      val result = toRun; toRun = -1; return result;
    }

  } // end of Facet

  // -------------------------------------------------------
  /* The Arbitrator object, used to resolve races between timeouts and commits
  */
  private object Arbitrator{
    private var status = INIT; 
    // status is either INIT, TIMEDOUT (when MainAlt has timed out), or COMMIT
    // (when Facet has received a commit). 

    /* Check whether a race has occurred between a commit and a timeout.
       @param s one of INIT, TIMEDOUT or COMMIT
       @return  a return value of false means that the calling process lost a
       race between a TIMEDOUT and a COMMIT.
    */
    def checkRace(s:Int) : Boolean = synchronized{
      if(s==INIT){ status = INIT; return false; }
      else if(s==TIMEDOUT){
        if(status==INIT){ status=TIMEDOUT; return true; }
        else{ assert(status==COMMIT); return false; }
      }else{
        assert(s==COMMIT);
        if(status==INIT){ status=COMMIT; return true; }
        else{ assert(status==TIMEDOUT); return false; }
      }
    }
  }

} // end of class


/** Alt object */

object Alt{ 
  /** Result returned by Alt.Event.register or Alt.commit to indicate that 
      a branch should be fired.  */
  val YES = 0; 
  /** Result returned by Alt.Event.register or Alt.commit to indicate that 
      a branch should not be fired.  */
  val NO = 1; 
  /** Result returned by Alt.Event.register or Alt.commit to indicate that 
      a branch should not be fired, but it might be possible to fire it in 
      the near future.  */
  val MAYBE = 2; 
  /** Result returned by Alt.Event.register to indicate that the channel 
      has closed */
  val CLOSED = 3; 

  trait Syntax { 
    def apply() : Unit = new Alt(elements) apply
  
    def length:   Int
    def elements: Seq[Event] = { 
      val r = new Array[Event](length); copy(0, r); r
    }
    
    def copy(n: Int, v: Array[Event]): Int
    def | (b: Syntax)       : Syntax  = new Join(this, b)
    def | (t: Event)        : Syntax  = this | new Singleton(t)
    def | (t: Seq[Event])   : Syntax  = this | new Collection(t)    
  } // end of Syntax
  
  private class Singleton(it: Event)  extends Syntax{ 
    def length = 1
    def copy(n: Int, v:Array[Event]) = { v(n)=it; n+1 }
    override def toString = it.toString
  }

  private class Collection(them: Seq[Event])  extends Syntax{ 
    def length = them.length
    def copy(n: Int, v:Array[Event]) = { 
      for (i<-0 until length) v(n+i)=them(i); n+length }
    override def toString = { 
      var s = ""
      for (it <- them){ if (s!="") s+= " | "; s+=it.toString }
      s
    }
  }
  
  private class Join(l: Syntax, r: Syntax) extends Syntax{
    def length = l.length+r.length
    def copy(n: Int, v:Array[Event]) = { r.copy(l.copy(n, v), v) }
    override def toString = l.toString + " | " + r.toString
  }

  def  toSyntax(ev: Alt.Event): Syntax = new Singleton(ev)
  def  toSyntax(evs: Seq[Alt.Event]): Syntax = new Collection(evs)

  /** `Event`s are composed with `|` and embedded in
      `Alt`s. */

  abstract class Event(_cmd: ()=>Unit, _guard: ()=>Boolean)
  {
    val cmd       = _cmd
    val guard     = _guard
    
    /** Abstract syntax of the alt of this and `other` */
    def | (other:  Event)      : Alt.Syntax = Alt.toSyntax(this) | other
          
    /** Abstract syntax of the alt of this and 
        `others` */
    def | (others: Seq[Event]) : Alt.Syntax = Alt.toSyntax(this) | others  
    
    /** Have an alt register with this event.
        @param a the alt
        @param n the index of the branch 
        @return one of YES, NO, MAYBE, CLOSE, TIMEOUT or ORELSE
    */ 
    def register(a:Alt, n:Int) : Int 
    /** Have an alt deregister with this event.
        @param a the alt
        @param n the index of the branch 
    */ 
    def deregister(a:Alt, n:Int) 
  }
  
  /** A timeout guard is a precursor to a timeout event */
  case class TimeoutGuard(timeMS: () => Long){
    def ==> (cmd: => Unit) = new TimeoutEvent(timeMS, ()=>cmd)
    def --> (cmd: => Unit) = new TimeoutEvent(timeMS, ()=>cmd)
  }

  /** A timeout event */
  case class TimeoutEvent(timeMS: ()=>Long, override val cmd: ()=>Unit) 
       extends Event(cmd, (()=>true)) {
    override def register(a:Alt, n:Int) : Int = { 
      throw new RuntimeException("TimeoutEvent.register"); 
    }
    override def deregister(a:Alt, n:Int) = { } // no nothing
  }
  
  /** An orelse guard is a precursor to an orelse event */
  case object OrElseGuard { 
    def ==> (cmd: => Unit) = new OrElseEvent(0, ()=>cmd) 
    def --> (cmd: => Unit) = new OrElseEvent(0, ()=>cmd) 
  }
  
  /** An orelse event.  (The first parameter circumvents a compiler bug
      in 2.7.1-final that caused the compiler to fail catastrophically)
  */  
  case class OrElseEvent(_X_ : Int, override val cmd: ()=>Unit) 
       extends Event(cmd, (()=>true)) {
    override def register(a:Alt, n:Int) : Int = { 
      throw new RuntimeException("OrElseEvent.register");
    }
    override def deregister(a:Alt, n:Int) = { } // no nothing
  }

}









