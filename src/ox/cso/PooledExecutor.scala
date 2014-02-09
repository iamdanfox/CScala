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
import  scala.collection.mutable.HashSet

/**
        A pooled-thread execution mechanism that keeps its server threads around
        for <code>keepSecs</code> seconds after they become idle.
        If <code>isDaemon</code> is true, then the threads that are
        made have their daemon bit set before being started.
        <p>
        If the runtime JVM property <code>ox.cso.pool.instrument</code> is
        <code>1</code> then when the JVM terminates a report will be
        made on the error stream indicating the extent to which 
        pooled threads were used.
        <p>
        If the value of the runtime JVM property <code>ox.cso.pool</code> 
        is a positive number, then the keepalive time for idle pooled threads is
        set to that number of seconds; if it is unset, or set but is not a number, then 
        a default period (4 seconds) is used; if it is set to zero
        then threads are not pooled. 
        
        @see ThreadHandle
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 634 $ 
 $Date: 2013-04-22 22:30:15 +0100 (Mon, 22 Apr 2013) $
}}}
*/


class PooledExecutor(keepSecs: Int, isDaemon: Boolean) extends Executor
{  
   /** 
       The set of servers that have not yet timed out and are waiting
       to be given work.
       <p>
       Invariant: every <code>Server</code> in the pool has already run
       at least one job.
   */
   private val pool = new HashSet[Server]
   
   def poolEmpty : Boolean = synchronized { pool.isEmpty }
   
   /** Add a server to the pool 
   */
   def addServer(server: Server) = synchronized
   {  
       if (instrumentingretire) System.err.println("[Pooling "+server.toString+"@"+pool.size+"]")
       pool.add(server)
   }
   
   /** Remove a server from the pool */
   def removeServer(server: Server) = synchronized
   {   server.finish
       if (instrumentingretire) System.err.println("[Retiring "+server.toString+"]")
       pool.remove(server)
   }
   
   /** Fetch a server from the pool 
   */
   def fetchServer : Server = synchronized 
   {   val server = pool.last
       if (instrumentingretire) System.err.println("[Fetching "+server.toString+"]")
       pool.remove(server)
       server
   }
   
   /**
        Time (in ms) that a server can be dormant
   */
   private val timeout = keepSecs * 1000
   
   /**  Statistic */
   private var executeCount, poolCount, retireCount = 0L 
   
   def incExecute = synchronized { executeCount+=1 }  
   def incPool    = synchronized { poolCount+=1 }  
   def incRetire  = synchronized { retireCount+=1 }  
   
   import java.lang.System.{getProperty}
   import java.lang.Runnable
   
   private val instrumenting       = getProperty("ox.cso.pool.instrument")=="1"
   private val instrumentingretire = getProperty("ox.cso.pool.instrument.retire")=="1"
   
   override def toString = if (isDaemon) "Daemon pool" else "Process pool"
   
   /**  Report process pooling statistic to stderr as:
   
            satisfied from pool / total executed / total retired 
   */
   def  report() : Unit =
   { if (instrumenting) 
     { System.err.println("["+this+": "+poolCount+"/"+executeCount+"/"+retireCount+"]")
       shutdown
     }
   }
   
   if (instrumenting) 
   { java.lang.Runtime.getRuntime.addShutdownHook(
       new Thread("Pooled Executor Instrument for "+this) { override def run = report } 
     )
   }
   
   /**
       If the pool is empty then start a new server working
       on this job; otherwise start one of the waiting servers.
   */
   def execute(runnable: Runnable) = synchronized
   { incExecute
     if (poolEmpty) 
       { val s = new Server
         s.start(runnable)
       }
       else
       { fetchServer.reStart(runnable)
         incPool
       }
   }
   
   /**
        Terminate all currently-dormant servers. This does not stop
        the pool from functioning, but if called at the end of a
        program can result in faster exit of the program
        from the JVM, because the JVM does not need to wait
        for the remaining dormant non-daemon server threads to terminate. 
        The usual way to call this method is via <code>CSO.exit</code>.
   */
   override def shutdown = synchronized
   { while (!poolEmpty) fetchServer.finish() }

   /**
        A <code>put/get</code> pair whose <code>get</code> is bounded
        by the given timeout.  If the timeout elapses then a null
        is returned.
   */
   class WaitVar(timeout: Long)
   { private var value: Runnable = null
     private var defined  = false
     
     def get: Runnable = 
         synchronized
         { if (!defined) wait(timeout)
           val r   = value 
           value   = null
           defined = false
           return r
         }
     
     def abandon : Unit = synchronized
     { value   = null
       defined = true
       notify
     }
     
     
     def put(it: Runnable) : Unit = 
         synchronized
         { value   = it
           defined = true
           notify
         } 
   }
   
   /**
        A <code>Server</code> is a thread that repeatedly gets a
        runnable; runs it; then puts itself back in the pool of
        servers and waits for another runnable. If it times out
        while awaiting work then it removes itself from the pool.
   */
   class Server extends java.lang.Thread
   { 
     val work    = new WaitVar(timeout)
     var started = false
     var uses    = 0L
     
     val hcode   = super.hashCode
     override def hashCode = hcode
     
     /** 
         Start this thread working on the given <code>Runnable</code>. 
     */
     def start(runnable: Runnable)
     { if (!started) 
       { setDaemon(isDaemon)
         started = true
         start() 
       }
       work.put(runnable)
     }
     
     /** 
         Start this thread working on the given <code>Runnable</code>. 
     */
     def reStart(runnable: Runnable)
     { 
       work.put(runnable)
     }
     
     def finish() = synchronized
     { 
       work.abandon
       finalize()
     }
     
     /**
        Repeatedly gets a task, runs it, goes back to the pool.
        If the task throws an exception, then the server is
        allowed to terminate, because that is the only way 
        that the exception can percolate up the process
        tree.
     */
     override def run 
     { val originalName = java.lang.Thread.currentThread.getName
       try
       { 
         var task = work.get
         while (task != null)
         { uses += 1
           if (instrumentingretire) 
              System.err.println(originalName+"("+uses.toString+"): "+task.toString);
           task.run                                 // work
           task = null                              // for the gc
           addServer(this)                          // signal readiness for work
           if (instrumentingretire) System.err.println("[Waiting for work: "+this+"]")
           task = work.get                          // await work (or timeout)
         }
         // task is no Longer in use because it retired
         incRetire
         removeServer(this)
       }
       finally
       { 
       }
     }
     
     override def finalize()
     {
       if (instrumenting) Console.println(this + " " + " was reused: "+uses)
       super.finalize()
     }
   }
}






