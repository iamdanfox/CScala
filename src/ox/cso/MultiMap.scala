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


package ox
import  scala.collection.mutable._

/**

A `MultiMap[K,D]` represents a (total) mapping `m`
from `K` to `List[D]` that initially
maps all keys to `Nil`.
   
{{{
@author  Bernard Sufrin
@version 01.2006
$Id: MultiMap.scala 553 2012-08-25 12:22:48Z sufrin $
}}}

*/


class MultiMap[K,D] extends Function1[K,List[D]]
{ val rep = new HashMap[K,List[D]]
  /**
      ` m := m + { k -> d :: m (k) } `
  */
  def put(k:K, d:D) : Unit =
  { if (rep.contains(k)) rep.update(k, d :: rep.apply(k)) else rep.update(k, d :: Nil) } 
  
  /** 
      Returns ` m(k) `
  */
  def apply(k: K) = 
  { if (rep.contains(k)) rep.apply(k) else Nil
  }
  
  /**
      Print (on the given `PrintWriter`)
      the maplets that do not map to `Nil`      
  */
  def printOut(stream: java.io.PrintWriter) = stream.println(rep)
}




