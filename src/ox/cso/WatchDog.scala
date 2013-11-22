/*

Copyright © 2010 - 2012  Gavin Lowe, Oxford University

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
 A WatchDog  takes a trace specification <code>spec</code>,
 ''i.e.'' a function from sequences of events to Boolean. The
 specification is checked whenever a new event is logged.
 A <code>WatchDogException</code> (embedding the trace) is thrown if the
 specification is not satisfied.
 
{{{
 @version 03.20120824
 @author Gavin Lowe, Oxford
 $Revision: 554 $ 
 $Date: 2012-08-26 14:59:03 +0100 (Sun, 26 Aug 2012) $
}}}
*/
class WatchDog[E](spec : Seq[E] => Boolean)
{

  private var tr = new scala.collection.mutable.ListBuffer[E]; // the trace of events so far

  /** Log an event and re-evaluate <code>spec</code> */
  @throws(classOf[WatchDogException[E]])
  def log(e:E) = synchronized {
    tr += e;
    if (!spec(tr)) throw new WatchDogException[E](tr)
  }
}

/** 
    A stateful watchdog takes a predicate <code>ok</code> on states
    and a state transition function <code>update</code>.
    It maintains a trace of events and a state (initially <code>initS</code>) 
    that is updated for each logged event, after which
    <code>ok</code> tests whether the new state is
    acceptable.  A <code>StatefulWatchDogException</code> (embedding
    the state and the trace) is thrown if the state is not
    acceptable.
    
    @see Watchdog
    
    $Id: WatchDog.scala 554 2012-08-26 13:59:03Z sufrin $
*/
class StatefulWatchDog[S,E](initS : S, update : (S,E) => S, ok : S => Boolean) 
{
 
  private var state = initS; // the current state
  private var tr = new scala.collection.mutable.ListBuffer[E]; 
    // the trace of events so far
  
  /** Log an event and re-evaluate <code>ok</code> */
  @throws(classOf[StatefulWatchDogException[S,E]]) 
  def log(e:E) = synchronized {
    state = update(state,e); 
    tr += e;
    if (!ok(state)) throw new StatefulWatchDogException[S,E](state, tr)
  }
}
 
case class WatchDogException[E](_trace: Seq[E]) extends RuntimeException("Illegal Trace")
{ val trace = _trace
} 

case class StatefulWatchDogException[S,E](_state: S, _trace: Seq[E]) 
      extends RuntimeException("Illegal State")
{ val trace = _trace
  val state = _state
} 






