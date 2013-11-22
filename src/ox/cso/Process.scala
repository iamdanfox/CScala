/*

Copyright © 2007 - 2012  Bernard Sufrin, Worcester College, Oxford University

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
  A <code>Process</code> is effectively a procedure that may be
  run in the current thread, may be forked to run in a thread
  separate from the current thread, or may be composed 
  with another <tt>Process</tt> to form a parallel composite process.
    <p>
    <code>p.fork</code>   
                starts the process <code>p</code> as a new thread and
                returns the corresponding <code>ThreadHandle</code>
    
    <p>
    <code>p.forkdaemon</code>  
                starts the process <code>p</code> as a new daemon thread and
                returns the corresponding <code>ThreadHandle</code>
    
    <p>
    <code>p()</code>      
                runs the body of the process <code>p</code> in
                the invoking thread.  
    <p>
    <code>p || q</code>  
                represents a composite process that terminates when <code>p</code>
                and <code>q</code> have terminated (either cleanly
                or by throwing an uncaught exception). When it is
                started, <code>p</code> runs in the invoking thread,
                and <code>q</code> runs in a separate thread.
                <ul>
                <li>
                If neither component terminates with an exception
                then the composite terminates cleanly.
                </li>
                <li>
                If one component terminates with a non-<tt>Stop</tt>
                exception then the composite re-throws that exception when
                they have both terminated. 
                </li>
                <li>
                If both components terminate with 
                non-<tt>Stop</tt> exceptions then a <tt>ParException</tt>
                is raised which embodies them both. 
                </li>
                <li>
                If both
                components terminate with <tt>Stop</tt> exceptions
                then a <tt>Stop</tt> exception is raised. 
                </li>
                </ul>
  <p>
  If a process terminates with a non-<tt>Stop</tt> exception then
  a stack backtrace will be printed, unless there is a
  java runtime property named <tt>ox.cso.process.traceexception</tt>
  with value <tt>off</tt>.
   
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}} 

TODO: Do thread groups make sense?

**/
class Process(name: String)(body: () => Unit) extends Function0[Unit]
{ var theName     = Process.genName(name)

  /** Run this process in a new Java thread: if the thread
      invoking this is a daemon, then  the new thread will also
      be a daemon.
  */
  def fork : ThreadHandle = 
      { val thread = ThreadHandle.NEW(theName, false, body)
        thread.start
        return thread
      }
      
  /** 
      Run this process in a new daemon Java thread. All threads
      spawned by daemon threads are treated as daemons.
  */
  def forkdaemon : ThreadHandle = 
      { val thread = ThreadHandle.NEW(theName, true, body)
        thread.start
        return thread
      }

  /** Construct the parallel composition of this and a process abstracted from body. 
      @since Revision 410
  */
  def || (body: => Unit) : Process = this || new Process (null) (()=>body)
    
  /** Construct the parallel composition of this and other.  */
  def || (other: Process) : Process = 
      new Process (theName +" || "+ other.theName) ( () => 
          { val otherHandle = other.fork
            var thrown : Throwable = null
            var clean = true
                        
            try   { body() } 
            catch { case stopped: Stop => { thrown = stopped }
                    
                    case other: java.lang.InterruptedException => 
                    { thrown = other
                      clean = false
                      otherHandle.interrupt 
                    }
                    case other => 
                    { if (Process.traceExn) other.printStackTrace
                      thrown = other
                      clean = false 
                    }
                  }
            
            (thrown, otherHandle.getThrown) match
            { case (null,              null)              => ()
              case (null,              stopped@Stop(_,_)) => throw stopped
              case (stopped@Stop(_,_), null)              => throw stopped
              case (stopped@Stop(_,_), Stop(_,_))         => throw stopped
              case (null,              otherR)            => throw otherR
              case (otherL,            null)              => throw otherL
              case (otherL,            otherR)            => throw new ParException(theName, otherL, other.theName, otherR)
            }
          })
          
  /** Run this process in the current thread */
  def apply() = body()
  
  /** Set the name of this process, and return this process */
  def withName(name: String): Process = { theName = name; return this } 
}

/** 
   A <code>Thread</code> that records whether it terminated cleanly or not.
*/

class ThreadHandle(name: String, daemon: Boolean, body: () => Unit) extends java.lang.Runnable
{ private var terminated = false
  private var status     = true
  private var thrown : Throwable = null
  private var thread : Thread    = null
  
  private def terminate(status: Boolean) = synchronized 
  { this.status = status
    terminated = true
    notifyAll
  }
  
  /** Interrupt this running thread; also Interrupt its 
      concurrently-running component if it is a ||.
  */
  def interrupt     = { thread.interrupt }
  
  /**
      Return true if the current thread was Interrupted, and clear its
      Interrupted status.
  */
  def interrupted : Boolean  = java.lang.Thread.interrupted 
  
  /** Wait for the thread to terminate, then return its status (true
      for clean termination (false otherwise). Clean termination
      is defined as normal termination, or termination
      by throwing a subtype of <code>Stop</code>/.
      <p>
      Exceptions are propagated through || -- but non-<code>Stop</code>
      exceptions are propagated in preference to <code>Stop</code> exceptions.
  */
  def isClean = synchronized { while (!terminated) wait; status } 
  
  def isCleanException = isClean && (thrown!=null)
  
  /** Wait for the thread to terminate, then return the throwable that
      terminated it (or null if it terminated without an exception). 
  */  
  def getThrown    = synchronized { while (!terminated) wait; thrown } 
  
  /** Returns true iff the thread has terminated */
  def isTerminated = synchronized { terminated } 
  
  /** Start executing */
  def start = ThreadHandle.execute(this)
  
  /** Is this a daemon process */
  def isDaemon = daemon
  
  /** The name of the process */
  def getName = name
    
  override def run = 
  { var originalName = ""
    try   { thread   = java.lang.Thread.currentThread
            originalName = thread.getName
            thread.setName(name)  
            body()
            terminate(true)
          } 
    catch { case thrown@Stop(_,_) => { terminate(true);  this.thrown = thrown } 
            
            case other            => 
            { if (Process.traceExn) other.printStackTrace
              terminate(false)
              this.thrown = other 
            }
          }
    finally
          { thread.setName(originalName)            
            thread.setPriority(java.lang.Thread.NORM_PRIORITY)          
          }
  }  
}

/**
    Thrown when a || terminates with an exception in both branches.
    The diagnostic backtrace needs to be improved.
    
    @see Process
*/
class ParException(lname: String, causeL: Throwable, rname: String, causeR: Throwable) 
      extends 
      java.lang.RuntimeException("from " + lname + " || "+ rname, causeL) 
{ 
   override def printStackTrace(writer: java.io.PrintStream)
   { 
     super.printStackTrace(writer)
     writer.print("And by exception from: "+ rname+": ")
     causeR.printStackTrace(writer)
   }
}

/** 
    ThreadHandle is the factory object for ThreadHandles.  If the
    runtime system property <code>ox.cso.pool</code> is set,
    to a number > 0 then threads are pooled; if
    it is left unset, then threads are pooled and kept alive 
    for 4 seconds, otherwise whenever a thread is needed
    to execute a process it is obtained by constructing a new one.
    <p>
    It is not necessary to use pooled threads in a CSO program, but
    it can make some programs run considerably faster -- particularly
    those that use very many short-lived processes.
    <p>
    The price of using pooled threads is that at the end of the
    program execution there can be a delay while the pooled threads
    that are currently unused drop out of the pool. 
    The method <code>CSO.exit</code> speeds this up 
    when threads are pooled. It should be called as the last thing a 
    CSO program does.
    <p>
    
    If the value of the runtime JVM property <code>ox.cso.pool</code> 
    is a positive number, then the keepalive time for idle pooled threads is
    set to that number of seconds; if it is unset, or set but is not a number, then 
    a default period (4 seconds) is used; if it is set to zero
    then threads are not pooled. 
    
    @see Process
{{{    
    $Id: Process.scala 553 2012-08-25 12:22:48Z sufrin $
}}}
   
*/

object ThreadHandle
{  
   
   /** keepAlive time for pooled threads */
   val poolTime = java.lang.System.getProperty("ox.cso.pool")
   /** True if a pool time as specified */
   val exPoolTime = poolTime!=null
   /** keepAlive time */
   val keep = if (exPoolTime && poolTime.matches("[0-9]+")) Integer.parseInt(poolTime) else 4;         
   /** An executor -- pooled if the keepAlive time is > 0 */
   val threads : Executor = if (keep>0) new PooledExecutor(keep, false) else new UnpooledExecutor(false)
   /** An executor for daemons -- pooled if the keepAlive time is > 0  */
   val daemons : Executor = if (keep>0) new PooledExecutor(keep, true)  else new UnpooledExecutor(true)
                    
   /** 
       Run the process embodied in the ThreadHandle -- using a
       pooled thread if necessary.
   */
   def execute(threadHandle: ThreadHandle) = 
       (if (threadHandle.isDaemon) daemons else threads).execute(threadHandle)
   
   def NEW(name: String, daemon: Boolean, body: () => Unit) : ThreadHandle =
   {
      new ThreadHandle(name, daemon || java.lang.Thread.currentThread.isDaemon, body)
   }
   
   def exit : Unit = { threads.shutdown; daemons.shutdown }
}

object Process 
{ private var processes = 0
  private def genName(name: String) = 
  { processes = processes+1
    if (name==null) ("Process-"+processes.toString) else name
  }
  var traceExn = System.getProperty("ox.cso.process.traceexception") != "off"
}




































