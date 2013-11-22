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
  
  A finitely buffered point-to-point Chan implementation. Point-to-point
  condition is not dynamically checked. 
  
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
*/
class   OneOneBuf [T: Manifest] (size: Int) 
extends BufImp [T](size) 
with    UnSharedInPort[T]
with    UnSharedOutPort[T]
{
  name = BufImp.newName("OneOneBuf")
  /** The channel is closed */
  override def closein  = { close }
  /** The channel is closed */
  override def closeout = { close }
}


















