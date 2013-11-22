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
Unpooled thread-execution mechanism that yields a brand new thread
for each process execution. This is usually very costly, and unless
there are good reasons for using this implementation it is better to
use 'PooledExecutor'. The method of specifying pooling is documented in 
`ThreadHandle`.

@see ThreadHandle
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/

class UnpooledExecutor(isDaemon: Boolean) extends Executor
{  
   def execute(runnable: java.lang.Runnable)
   { val t = new Thread(runnable)
     t.setDaemon(isDaemon)
     t.start
   }
}




