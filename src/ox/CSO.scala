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



package ox;
import  ox.cso._
import  scala.language.postfixOps /* From scala v10.0 */



/**


{{{
@author  Bernard Sufrin, Oxford
@version 03.20120824
$Revision: 643 $ 
$Date: 2013-11-04 18:21:46 +0000 (Mon, 04 Nov 2013) $
}}}


This module exports the main types and control constructs
of the CSO (Communicating Scala Objects) notation. It is
our Intent to provide a notation for communicating objects
that is in the spirit of the OCCAM and Eclectic CSP
languages.


A brief summary of the ideas involved appears below, but
much of the detail of the implementation is hidden in
classes in the package `ox.cso`.

== Processes ==
=== Notation ===
The simple process expression 

{{{
        proc { commands }
}}}

yields a new ''PROC''ess. If you would like a more compact
notation and can edit unicode, you can write this as
{{{
        π { commands }
}}}

The parallel composition process expressions

{{{
        PROC1 || PROC2
}}}
    
and (more generally)     

{{{ 
        PROC1 || PROC2 || ... || PROCn
}}}  

yield a new process; as does the prefix form:

{{{
        || (for (var <- ITERABLE) yield procexpression(var)
}}}
  
=== Evaluation === 

If `PROC` is a process, then it can be run in one of three ways
 - `PROC()` -- which has the following effects:
   - A simple process executes its ''commands'' in the current thread
   - A composite (parallel) process: `PROC1 || PROC2`
     runs `PROC`1 in a new thread, 
     runs `PROC`2 in the current thread, and
     terminates when both `PROC`1 and `PROC`2 have terminated.        
   - Parallel composition is essentially associative, so 
     `PROC1 || PROC2 || ... || PROCn`
     runs its component processes in ''n-1'' new threads
     and terminates when they have all terminated.
     
 - `PROC.fork` -- which starts running `PROC` in a 
   new thread, and returns a ''handle'' for that thread.

 - `PROC.forkdaemon` -- which starts running `PROC` in a 
   new ''daemon'' thread, and returns a ''handle'' for that thread.
   
   The java documentation explains that daemon threads are ''long-lived''
   threads set up to provide services for other threads. As Long as
   there are non-daemon processes running in a program a daemon may be
   called on to serve one of them; when only daemons are left, the Java
   runtime system terminates the program.


==== Syntactic Sugar ====
Within the scope of the ox.CSO declarations and 
in a context that requires a simple value of type ''PROC''ess,
a ''Unit''-valued expression will be coerced to a
process.
This notational device does not work within the '''yield'''-expressions
of iterators.


== Channels ==
 
A channel communicates data from its input to its output
port. There are several types of channel implementation.
All implementations are point-to-point (in the sense that
each datum is communicated from a single process to a single
process).


{{{
        chan!value
}}}
writes the ''value''
to the output port of the channel. The value can
subsequently be read from the input port of the
channel by  
{{{
        chan?
}}}


All but `Buf` channels are synchronized (in the
sense that both communicant processes are suspended until
data has been transferred).       


Synchronized channel implementations differ from each other only insofar as
the extent to which they permit contention between processes at the sending
(writer) and receiving (reader) ends. Implementations that
permit no contention at their reading (respectively writing)
end perform a partial dynamic check that ''can'' catch
two processes in contention to read (respectively write)
from a writing (respectively reading) process that is not
keeping up with them. ''This check -- the best that can be
done without static enforcement of a non-sharing rule --
is sound, but not complete.''

An ''extended rendezvous read'' takes the form
{{{
        chan?function
}}}
In this case, the given function is applied to the
value read from the input port (when it arrives), and
the writing process is suspended until the computation
of the function is complete.        
 
=== Ports ===
In fact a channel that transmits data of type
`T` implements the methods both of an `OutPort[T]` and an
`InPort[T]` (abbreviated to `![T]` and `?[T]` respectively),
and `Chan[T]` is therefore a subtype of both these types.
So within the scope of the declarations `outport: ![T]`
and `inport: ?[T]` the following expressions are well-typed.
 
{{{
        outport!value
        inport?
        inport?function
}}}
 

== Alternation ==

=== Semantics ===
The detailed semantics of alternations is explained in the
documentation of the `Alt` class.

=== Notation by example ===


{{{ 
   alt ( ((counter>0) &&& left)   ==> { x  => print(x); print(" "); counter -= 1}
       | ((counter==0) &&& reset) ==> { () => println(); counter = 10 }
       )
}}}
If the counter is positive and the left channel is open then
read a value from `left`, print it, then decrement
the counter. If the counter is zero then read a () from the
reset channel, print a newline, and restart the counter at 10.    


{{{
   serve ( 
         | (for (port<-ports) yield  port ==> { x => println(x) })
         | quit ==> 
           { () => for (port<-ports) port.closein } 
         )
}}}
Repeatedly read and print a value from the one of the `ports`
that are ready (or become ready) to be read; or close them all
in response to reading a () from `quit`.


{{{
   serve (
         | (for (port<-ports) yield port ==> { x => println(x) }
         | after(400) ==> { println("------------") }
         | orelse     ==> { println("============"); stop }
         )
}}}
Repeatedly read and print values from any of the ports that are ready.
If there is ever a period of 400ms during which no port becomes ready,
then print a line of dashes; and once all the ports have closed
print a line of equals signs and stop. 

{{{
   val buffer = ...
   serve ( ((!buffer.isFull) &&& in)   -?-> { buffer.enqueue(in?) }
         | ((!buffer.isEmpty) &&& out) -!-> { out!buffer.dequeue }
         )
}}}
Act as a buffer for the data flowing between `in` and `out`.  

=== Connection Events ===
A '''Connection''' is effectively a pair of channels between a client and a server.

To use boolean guarded alt events with client or server connections (as
specified in `ox.cso.Connection`) there is a syntactic sugar
that makes it possible to specify whether one is guarding the
output or input port of the connection. Here's a (seminonsensical)
alt loop that collects results from servers and passes them
back to arbitrarily-chosen clients.


{{{
   priserve (
            | (for (server <- servers) yield 
                       ((busy>0) &&&? server) ==>
                       { result => results += result
                                   freeservers += server
                                   busy -= 1  })
            | (for (client <- clients) yield 
                   ((!results.isEmpty) &&&! client) -!-> { client!(results.pop) }) 
            | (for (client <- clients) 
                   ((!freeservers.isEmpty) &&&? client) ==>
                   { job => { val server = freeservers.pop
                              busy += 1
                              server!(client?)
                   }
            )
}}}  

If you don't want to use this sugar, or if the guards are unconditional then
use a cast. For example:
{{{
   ((busy>0) &&& server.asInstanceOf[?[Reply]])                 ==>  ...
   ((!results.isEmpty) &&& client.asInstanceOf[![Reply]])       -!-> ...
   ((!freeservers.isEmpty) &&& client.asInstanceOf[?[Request]]) ==>  ...
   server.asInstanceOf[?[Reply]]   ==>  ...
   client.asInstanceOf[![Reply]]   -!-> ...
   client.asInstanceOf[?[Request]] ==>  ...
}}}

*/

object CSO
{  
   /** Prototype of a process.
       @see ox.cso.Process
   */
   type PROC    = ox.cso.Process
   
   /** An input port. 
       @see ox.cso.InPort 
   */
   type InPort[+T]  = ox.cso.InPort[T]
   
   /** Alternation syntax 
       @see ox.cso.Alt 
   */
   type AltSyntax  = Alt.Syntax
   
   /** An input port. 
       @see ox.cso.InPort 
   */
   type ?[+T]  = ox.cso.InPort[T]
   
   /** A shared input port. 
       @see ox.cso.SharedInPort 
   */
   type #?[+T]  = ox.cso.SharedInPort[T]
   
   /** An output port. 
       @see ox.cso.OutPort 
   */
   type OutPort[-T] = ox.cso.OutPort[T]
   
   /** An output port. 
       @see ox.cso.OutPort 
   */
   type ![-T] = ox.cso.OutPort[T]
   
   /** A shared output port. 
       @see ox.cso.SharedOutPort 
   */
   type #![-T] = ox.cso.SharedOutPort[T]
   
   /** A shared input port. 
       @see ox.cso.SharedInPort 
   */
   type SharedInPort[+T]  = ox.cso.SharedInPort[T]
   
   /** A shared output port. 
       @see ox.cso.SharedOutPort 
   */
   type SharedOutPort[-T] = ox.cso.SharedOutPort[T]
   
   /** An unshared input port. 
       @see ox.cso.SharedInPort 
   */
   type UnSharedInPort[+T]  = ox.cso.UnSharedInPort[T]
   
   /** An unshared output port. 
       @see ox.cso.UnSharedOutPort 
   */
   type UnSharedOutPort[-T] = ox.cso.UnSharedOutPort[T]
   
   /** 
       A channel (communicating data from its input to its output
       port). 
       @see ox.cso.Chan 
   */
   type Chan[T]    = ox.cso.Chan[T]
   
   /** A channel implementation type: single reader; single writer 
       @see ox.cso.OneOne
    */
   type OneOne[T]  = ox.cso.OneOne[T]
   /** A channel implementation type: many writers; one reader 
       @see ox.cso.ManyOne
   */
   type ManyOne[T] = ox.cso.ManyOne[T]
   /** A channel implementation type: one writer; many readers  
       @see ox.cso.OneMany
   */
   type OneMany[T] = ox.cso.OneMany[T]
   /** A channel implementation type: many writers; many readers  
       @see ox.cso.ManyMany
   */
   type ManyMany[T] = ox.cso.ManyMany[T]
   /** A channel implementation type 
       @see ox.cso.Buf
   */
   type Buf[T] = ox.cso.Buf[T]
      
   /** A process with the given body */
   def proc (body: => Unit) : PROC = new Process (null) (()=>body)
   /** A process with the given body */
   def π (body: => Unit) : PROC = new Process (null) (()=>body)
      
   /** A named process with the given body */
   def proc (name: String) (body: => Unit) : PROC = new Process (name) (()=>body)
   /** A named process with the given body */
   def π (name: String) (body: => Unit) : PROC = new Process (name) (()=>body)
   
   /** Coerce a Unit-valued expression Into a process */
   implicit def UnitToProc(body: => Unit) : PROC = new Process (null) (()=>body)
 
   /** The process that simply terminates */
   val skip : PROC                 = proc {}
   
   /** Construct a process from the given body, and run it  
       in a thread concurrently with the current process
   */
   def fork(body: => Unit) : ox.cso.ThreadHandle = 
       new Process (null) (()=>body) . fork
      
   /** Construct a process from the given body, and run it  
       in a daemon thread concurrently with the current process
   */
   def forkdaemon(body: => Unit) : ox.cso.ThreadHandle = 
       new Process (null) (()=>body) . forkdaemon

   /** The parallel composition of a collection of processes */
   def ||   (collection: Iterable[PROC]) : PROC = || (collection.iterator)
   
   /** The parallel composition of an iterator of processes */
   def ||   (processes: Iterator[PROC]) : PROC =
   { var r = if (processes.hasNext) processes.next else skip
     for (p <- processes) r = r || p
     r
   }
                      
   /** Construct and execute the alternation of a bunch of  events */
   def alt(syntax: AltSyntax) : Unit = new Alt(syntax.elements, false) apply
   
   /** Construct and execute the alternation of a bunch of events */
   def alt(events: Iterable[Alt.Event]) : Unit = new Alt(events) apply
   
   /** Construct and execute the prioritized alternation of a bunch of events */
   def prialt(events: Iterable[Alt.Event]) : Unit =  new Alt(events, true) apply
   

   /** Construct and execute the prioritized alternation of a bunch of events */
   def prialt(syntax: AltSyntax) : Unit = new Alt(syntax.elements, true) apply
   
   /** Construct the syntax of the alternation of a bunch of events */
   def |(events: Iterable[Alt.Event]) : AltSyntax = Alt.toSyntax(events)

   /** Timeout guard for an alt */
   def after(waitMS: => Long) = new Alt.TimeoutGuard(()=>waitMS)
   
   /** Orelse guard for an alt */
   val orelse = Alt.OrElseGuard
   
   /** Yield a new OneOne */
   def OneOne[T]()     = new OneOne[T]()
   
   /** Yield a new named OneOne */
   def OneOne[T](name: String) = new OneOne[T](name)
   
   /** Yield an new array of new OneOnes */
   def OneOne[T](n: Int) = for (i<-upto(n)) yield new OneOne[T]
   
   /** Yield a new array of named new OneOnes */
   def OneOne[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new OneOne[T](name+"."+i.toString) 
   
   /** Yield a new ManyOne */
   def ManyOne[T] = new ManyOne[T]
   
   /** Yield a new array of new ManyOne */
   def ManyOne[T](n: Int) = 
       for (i<-upto(n)) yield new ManyOne[T]    
   
   /** Yield a new array of new named ManyOnes */
   def ManyOne[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new ManyOne[T](name+"."+i.toString)    
      
   /** Yield a new  OneMany */
   def OneMany[T] = new OneMany[T]
   
   /** Yield a new array of new OneMany */
   def OneMany[T](n: Int) = for (i<-upto(n)) yield new OneMany[T]    
   
   /** Yield a new array of new named OneMany */
   def OneMany[T](n: Int, name: String) = 
       for (i<-upto(n)) yield new OneMany[T](name+"."+i.toString)    
      
   /** Yield a ManyOne */
   def ManyMany[T] = new ManyMany[T]
   
   /** Yield a new array of new ManyMany */
   def ManyMany[T](n: Int) = for (_<-upto(n)) yield new ManyMany[T]    
   
   /** Yield a new array of new named ManyMany */
   def ManyMany[T](n: Int, name: String) = for (i<-upto(n)) yield new ManyMany[T](name+"."+i.toString)    
      
   /** Yield a new Buf(size) */
   def Buf[T: Manifest](size: Int) = new Buf[T](size)
   
   /** Yield a new array(n) of new Buf(size) */  
   def Buf[T: Manifest](size: Int, n: Int) = for (_<-upto(n)) yield new Buf[T](size) 
      
   /** Yield a new OneOneBuf(size) */
   def OneOneBuf[T: Manifest](size: Int) = new OneOneBuf[T](size)
   
   /** Yield a new array(n) of new OneOneBuf(size) */  
   def OneOneBuf[T: Manifest](size: Int, n: Int) = for (_<-upto(n)) yield new OneOneBuf[T](size) 
      
   /** Repeatedly execute `cmd` until it throws a
       `Stop`.
   */
   def repeat (cmd: => Unit) : Unit =
   { var go = true;
     while (go) try { cmd } catch { case ox.cso.Stop(_,_) => go=false }
   }
   
   /**
        Repeatedly apply an alternation constructed from the given `Alt`
        syntax. This is equivalent to:
        {{{
          {val a=new Alt(syntax); repeat { a() }}
        }}}
        Nondeterministic choices between guards are made as fairly
        as is practically possible.
        @see Alt
   */
   def serve (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements) repeat
   }
   
   def serve (events: Iterable[Alt.Event]) : Unit =
   { 
     new Alt(events) repeat
   }
   
   /**
        Repeatedly apply a priority alternation constructed from the given `Alt`
        syntax. This is equivalent to:
        {{{
          {val a=new Alt(syntax.elements, true); repeat { a() }} 
        }}}
   */
   def priserve (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements, true) repeat
   }
   
   /**
        @see serve
   */
   @deprecated(message="Use serve", since="022010") def repeatalt (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements) repeat
   }
   
   /**
        @see serve
   */
   @deprecated(message="Use serve", since="2010") def repeatAlt (events: Iterable[Alt.Event]) : Unit =
   { 
     new Alt(events) repeat
   }
   
   /**
       @see priserve   
   */
   @deprecated(message="Use priserve", since="022010") def repeatprialt (syntax: AltSyntax) : Unit =
   { 
     new Alt(syntax.elements, true) repeat
   }
   
   /** Repeatedly execute `cmd` while `guard`
       is true or until `cmd` throws a
       `Stop`.
   */
   def repeat (guard: => Boolean) (cmd: => Unit) : Unit =
   { var go = guard;
     while (go) try { cmd; go=guard } catch { case ox.cso.Stop(_,_) => go=false }
   }   
         
   /** Execute `cmd`. If it throws a `Stop`
       then execute `alt`.
   */
   def attempt (cmd: => Unit) (alt: => Unit) : Unit =
   { try { cmd } catch { case ox.cso.Stop(_,_) => alt } }

   /** Break out of a repeat, or fail an attempt -- by throwing
       a `Stop`
   */
   def stop = throw new ox.cso.Stop("stop", null)
   
   /** Convenience object for matching `ox.cso.Stop exceptions` */
   object Stop
   { def unapply(v: ox.cso.Stop) : Option[(String, Throwable)] =
         v match { 
           case ox.cso.Stop(s,t) => new Some((s,t)) 
           //case _ => None
         }
   }
   
   /** Convenience object for matching `ox.cso.Closed` exceptions */
   object Closed
   { def unapply(v: ox.cso.Stop) : Option[(String)] =
         v match { 
           case ox.cso.Stop(s, t) => if (v.isInstanceOf[ox.cso.Closed]) new Some((s)) else None
         }
   }
   
   /** Convenience object for matching `ox.cso.Abort` exceptions */
   object Abort
   { def unapply(v: ox.cso.Stop) : Option[Unit] =
         v match { 
           case ox.cso.Stop(s, t) => if (v.isInstanceOf[ox.cso.Abort]) new Some(()) else None
         }
   }
   
   /** Maps an iterable Into a sequence */    
   implicit def toSeq[T](it: Iterable[T]): Seq[T] =
   if (it.isInstanceOf[Seq[T]])
      it.asInstanceOf[Seq[T]]
   else
   { val seq = new scala.collection.mutable.ArrayBuffer[T]
     seq ++= it
     seq
   } 
   
   /** Maps a Boolean into a pre-guard as part of the formation of
       guarded I/O events of the form:
       {{{
       (Boolean &&& inport)  -->  { command }
       (Boolean &&& inport)  ==>  { command }
       (Boolean &&& inport)  ==>> { command }
       (Boolean &&& inport)  -?-> { command }
       (Boolean &&& outport) -!-> { command }
       }}}
   */
   implicit def toGuard(guard: => Boolean): PreGuard = new PreGuard(()=>guard)
   
   /** Syntactic precursor of a guarded I/O event */
   protected class PreGuard(guard: ()=>Boolean)
   { 
     def &&&[T](port: ?[T]): InPort.GuardedInPortEvent[T] = 
         new InPort.GuardedInPortEvent[T](port, ()=>port.isOpen && guard())
     
     def &&&[T](port: ![T]): OutPort.GuardedOutPortEvent[T] =
         new OutPort.GuardedOutPortEvent[T](port, ()=>port.isOpenForWrite && guard())

     def &&&?[Req,Rep](server: ox.cso.Connection.Server[Req,Rep]): InPort.GuardedInPortEvent[Rep] = 
         { val port = server.asInstanceOf[?[Rep]]
           new InPort.GuardedInPortEvent[Rep](port, ()=>port.isOpen && guard())
         }
         
     def &&&?[Req,Rep](client: ox.cso.Connection.Client[Req,Rep]): InPort.GuardedInPortEvent[Req] = 
         { val port = client.asInstanceOf[?[Req]]
           new InPort.GuardedInPortEvent[Req](port, ()=>port.isOpen && guard())
         }
         
     def &&&![Req,Rep](server: ox.cso.Connection.Server[Req,Rep]): OutPort.GuardedOutPortEvent[Req] = 
         { val port = server.asInstanceOf[![Req]]
           new OutPort.GuardedOutPortEvent[Req](port, ()=>port.isOpenForWrite && guard())
         }
         
     def &&&![Req,Rep](client: ox.cso.Connection.Client[Req,Rep]): OutPort.GuardedOutPortEvent[Rep] = 
         { val port = client.asInstanceOf[![Rep]]
           new OutPort.GuardedOutPortEvent[Rep](port, ()=>port.isOpenForWrite && guard())
         }
   }
   
  /** The currently-running thread sleeps for the specified time milliseconds) */
  def sleep(ms: Long) = Thread.sleep(ms)
  
  /** Threadpools (if any) are closed down cleanly. The effect of this is to
      terminate any ''resting'' pooled threads immediately. That does not
      in itself cause the currently-running program to exit; nor does
      it prevent new processes being run (and generating new threads).
      The correct code to exit a program immediately is:
      {{{
      { CSO.exit; System.exit(anInteger)}
      }}}
      Without the `CSO.exit` call the program waits
      for its resting pooled threads to die their natural deaths, and
      the length of time this will take depends on the ''keepalive'' time for 
      pooled threads -- set by JVM property
      `ox.cso.pool`
      
      @see PooledExecutor
  */
  def exit { ThreadHandle.exit }
  
  /** Coercion of an `Alt.Event` Into the syntax of an an Alt.
      This is to permit single-branched alts to be constructed quietly.      
  */
  implicit def EventToAlt(ev: Alt.Event): AltSyntax = Alt.toSyntax(ev)
    
  /** Implicit values */
  implicit val implicit_long:Long     = 0l
  implicit val implicit_int:Int       = 0
  implicit val implicit_ref:AnyRef    = null
  implicit val implicit_float:Float   = 0.0f
  implicit val implicit_double:Double = 0.0
  implicit val implicit_String:String = ""

   /** 
       Concise representation of the open Interval `[0..n)`, which
       ''unlike'' `o until n` has a `map`
       method that is evaluated non-lazily, and therefore behaves 
       appropriately as a generator within `for`  
       comprehensions.
   */
   def upto (n: Int) : Seq[Int] = 
       new Seq[Int] 
       {
         def iterator = new Iterator[Int] 
         {
             var i=0
             def next    = { val r = apply(i); i=i+1; r }
             def hasNext = i < n
           }
         
         def length       = n
         
         def apply(i: Int) = 
             if (i<n) i else throw new IndexOutOfBoundsException(i + ">=" +n)
             
         def map[T: Manifest](f: Int=>T) =
         { val r = new Array[T](length)
           var i = 0
           while (i<n) { r(i) = f(i); i+=1 }
           r    
         }

       }
       
   /** 
       Concise representation of the open Interval `[m..n)`, which
       ''unlike'' `o until n` has a `map`
       method that is evaluated non-lazily, and therefore behaves 
       appropriately as a generator within `for`  
       comprehensions.
   */
   class range(m: Int, n: Int) extends Seq[Int]
   {
     def iterator = new Iterator[Int] 
     {   var i=m
         def next    = { val r = i; i=i+1; r }
         def hasNext = i < n
     }
     
     def length       = n-m
     
     def apply(i: Int) =
         if (m+i<n) m+i else throw new IndexOutOfBoundsException(i + ">=" + length)
     
     def map[T: Manifest](f: Int=>T) =
     { val r = new Array[T](length)
       var i = m
       while (i<n) { r(i-m) = f(i); i+=1 }
       r    
     }
   }
   
   def range(m: Int, n: Int) : Seq[Int] = new range(m, n)

  /**
      A semaphore.  
      @see ox.cso.Semaphore 
  */
  type Semaphore = ox.cso.Semaphore;

  /** 
      A barrier.  
      @see ox.cso.Barrier 
  */
  type Barrier = ox.cso.Barrier;

  /** 
      A combining barrier.  
      @see ox.cso.CombiningBarrier 
  */
  type CombiningBarrier[T] = ox.cso.CombiningBarrier[T];
}



































































