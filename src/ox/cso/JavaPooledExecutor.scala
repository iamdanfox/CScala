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
A pooled thread-execution mechanism that uses the standard java utility thread pool.

This was constructed in order to demonstrate a mysterious bug with threading in 
the Mac OS/X Lion Java thread implementation that doesn't seem to be there in 
OS/X 10.6 (Snow Leopard). 

The method of specifying pooling is documented in `ThreadHandle`.

@see ThreadHandle
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 630 $ 
 $Date: 2013-02-28 19:46:52 +0000 (Thu, 28 Feb 2013) $
}}}
*/

import java.util.concurrent.Executors

class JavaPooledExecutor(keep: Int, isDaemon: Boolean) extends Executor
{  val pool = Executors.newCachedThreadPool()
   def execute(runnable: java.lang.Runnable)
   { pool.execute(runnable) }
   
   override def shutdown() = pool.shutdown() 
}





