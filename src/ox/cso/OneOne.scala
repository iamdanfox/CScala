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


package ox.cso;

/**
  
  An (Occam-style) one-to-one Chan implementation that ensures that
  <tt>!</tt> synchronises with <tt>?</tt>.

{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}} 
*/
class   OneOne [T] (id: String) 
extends SyncChan[T](id) 
with    UnSharedInPort[T] 
with    UnSharedOutPort[T]
{
  def this() = this(SyncChan.newName("OneOne")) 
}














