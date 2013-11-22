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
package ox.cso

/** 
A `Semaphore` has operations `up` and `down`.  If
`down` is called while the semaphore is in the down state, the
proces is blocked; a subsequent call to `up` unblocks the process.

{{{
@version 03.20120824
@author Gavin Lowe, Oxford
$Revision: 553 $ 
$Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}

*/

class Semaphore {
  private var isDown = false;

  /** Try to move the semaphore into the down position, blocking on the semaphore
      if it is already down. */
  def down = synchronized{
    while(isDown) wait();
    isDown = true;
  }

  /** Move the semaphore into the up position; if there is a process blocked
      on the semaphore then unblock one of them.  
  */
  def up = synchronized{
    isDown = false;
    notify();
  }
}




