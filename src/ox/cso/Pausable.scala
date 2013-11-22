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
package ox.cso
/** 
  A <code>Pausable</code> object is one that can be paused for a random amount of
  time, by the procedure <code>pause</code>.  Each call of <code>pause</code> doubles
  the maximum possible length of the pause.  <code>resetPause</code> resets this
  length.

{{{
 @version 03.20120824
 @author Gavin Lowe, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
  
*/
trait Pausable {
  var maxDelay = 1; // max time to sleep in a contention breaker

  /** Pause for a random amount of time */
  def pause {
    val delay = Pausable.random.nextInt(maxDelay); 
    if (maxDelay<Integer.MAX_VALUE/2) 
       maxDelay += maxDelay; 
    else 
       maxDelay = Integer.MAX_VALUE;
    // val t0 = System.currentTimeMillis();
    Thread.sleep(delay/1000000, delay%1000000);
    // totalPause += System.currentTimeMillis-t0
    // experiments from here on down
    // pauseCount += 1;
    // if(maxDelay>maxMaxDelay) maxMaxDelay = maxDelay
    // totalDelay += delay;
    // if(maxDelay>=16)
    //   println(Thread.currentThread()+
    //        ": maxDelay = "+maxDelay+
    //        " ; maxMaxDelay = "+maxMaxDelay+
    //        " ; pauseCount = "+pauseCount+
    //        " ; totalDelay = "+totalDelay+"ns"+
    //        " ; totalPause = "+totalPause+"ms"); 
  }

  /** Reset maximum length of pause to an initial value */ 
  def resetPause = maxDelay = 1;
}

/**
        Implements the private source of randomness that is shared by
        all Pausable objects.
*/
object Pausable {  
  private val random = new scala.util.Random
}



