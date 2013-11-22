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


package ox.cso
import  ox.CSO._
import  scala.collection.mutable.Queue
import  ox.cso.Connection.{Client,Server}

/**
A <code>[Req,Rep]</code> process-farmer is primed with a collection of 
<code>[Req,Rep]</code> interfaces to worker-servers. It dispatches 
each request that arrives from its client (or on its input port) to the next
free worker. When a response is returned from a worker, it
is forwarded by the farmer to the client (or to the farmer's output port).
In short, it acts as a client to its servers, and as a server to
its clients.

{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/

object Farmer
{  /** Construct a farmer from the given workers; read requests from <code>in</code>
       and send replies to <code>out</code>.
   */
   def farmer[Req,Rep](workers: Seq[Server[Req,Rep]], in: ?[Req], out: ![Rep]) : PROC =
       farmer(in, out, workers)

   /** Construct a farmer from the given workers; read requests from 
       the client's request port and 
       and send replies to the client's response port.
   */         
   def farmer[Req,Rep](client: Client[Req, Rep], workers: Seq[Server[Req,Rep]]) : PROC = 
       farmer(client, client, workers) 
   
   /** Construct a farmer from the given workers; read requests from <code>in</code>
       and send replies to <code>out</code>.
   */
   def farmer[Req,Rep](in: ?[Req], out: ![Rep], workers: Seq[Server[Req,Rep]]) : PROC =  
   proc
   { var busy = 0                        // number of busy workers
     val free = new Queue[OutPort[Req]]  // queue of free worker connections
     free ++= workers                    // initially all workers are free
     // INVARIANT: busy+free.length=workers.length    
     serve (| (for (worker <- workers) yield 
                   ((busy>0) &&&? worker) ==>
                   { w => out  !  w
                          free += worker
                          busy = busy-1 
                   }
              )
            | ((free.length>0) &&& in) ==>
              { work => { val worker = free.dequeue
                          busy = busy+1
                          worker ! work
                        }
              }
           )
    
    ( proc { out.close }
    || || (for (worker <- workers) yield proc { worker.close })
    )()
  }
  
}


















