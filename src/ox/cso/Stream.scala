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
import  ox.CSO._;

/**
        <code>Stream</code> components support the systematic construction
        of (local) networks of processes.
        
        @see Components
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}
*/
object Stream
{ class VAR[T] { var value: T = _ }

  def zip[L,R,O](f:(L,R)=>O) (l: InPort[L], r: InPort[R], out: OutPort[O]) = 
  proc {serve (  l ==> { lv => out!f(lv, r?) }
              |  r ==> { rv => out!f(l?, rv) } 
              )
    out.closeout
    l.closein
    r.closein
  }
  
  def map[I,O] (f: I => O) (in: InPort[I], out: OutPort[O]) = 
  proc { 
    repeat { out!(f(in?)) }  
    out.closeout
    in.closein
  }
  
  def tee[T](in: InPort[T]) (outs: OutPort[T]*) (implicit t:T)=Tee(in)(outs)(t)
  
  def Tee[T](in: InPort[T]) (outs: Seq[OutPort[T]]) (implicit t:T) = 
  proc { 
    var v   = t
    val out = || (for (out <- outs) yield proc { out!v })
    repeat { v=in?; out() } 
    in.closein
    for (out <- outs) { out.closeout }
  }
  
  def merge[T](ins: InPort[T]*) (out: OutPort[T]) = Merge(ins)(out)
  
  def Merge[T](ins: Seq[InPort[T]]) (out: OutPort[T]) = 
  proc { 
    serve(| (for (in <- ins) yield in ==> { t => out!t }))
    out.closeout
    for (in <- ins) in.closein
  }
  
  def tail[T] (in: InPort[T], out: OutPort[T]) =
  proc { 
    in?;
    repeat { out!(in?) }  
    out.closeout
    in.closein
  }
  
  def copy[T] (in: InPort[T], out: OutPort[T]) =
  proc { 
    repeat { out!(in?) }  
    out.closeout
    in.closein
  }
  
  def prefix[T] (ts: T*) (in: InPort[T], out: OutPort[T]) =
  proc { 
    for (t <- ts) out!t
    repeat { out!(in?) }  
    out.closeout
  }
  
  def console[T](in: InPort[T]) = 
  proc { 
    repeat { Console.println(in?) } 
    in.closein
  }
  

  
}

















