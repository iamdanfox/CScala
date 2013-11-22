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
 A many-to-many synchronized channel. If you're sure that you don't
 need ''synchronization'', merely ''serialization'' then use a
 `Buf`.
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/
class ManyMany[T](name: String) extends SyncChan[T](name) with SharedOutPort[T] with SharedInPort[T]
{ val ilock = new AnyRef
  val olock = new AnyRef
  override def ?()             = 
       if (isOpen) 
           ilock synchronized super.?()
       else
          throw new Closed(name)
  override def ?[U](f:T=>U): U = 
       if (isOpen) 
           ilock synchronized super.?(f)
       else
          throw new Closed(name)
  override def !(obj: T) = 
       if (isOpen) 
           olock synchronized super.!(obj)
       else
          throw new Closed(name)
  /** No-op: the channel remains open */  
  override def closeout = {}
  /** No-op: the channel remains open */
  override def closein  = {}
  def this() = this(SyncChan.newName("ManyMany")) 
}











