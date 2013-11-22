/*

Copyright © 2007 - 12 Bernard Sufrin, Worcester College, Oxford University

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
import  ox.CSO.{?,!}
/**
Components that (mostly) work on finite or infinite streams of values
presented as ports. All components are designed to
terminate cleanly -- 'i.e.' to `closein` or `closeout` all the ports that they
communicate on in the appropriate direction for the type of port. 

Some of these components were inspired by (or copied from) components
from the Plug'n'Play collection of JCSP (withoutr necessarily
retaining the P'n'P names).

{{{
@version 03.20120824
@author Bernard Sufrin, Oxford
$Revision: 553 $ 
$Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
*/
object Components
{ /**
  Copy from the given input stream to the given output streams, performing
  the outputs concurrently. Terminate when the input stream or any of the
  output streams is closed.
  {{{
           in   /|----> x, ...
  x, ... >---->{ | : outs
                \|----> x, ...
  }}}
  */
  def tee[T](in: ?[T], outs: Seq[![T]]) = proc
  { var v = null.asInstanceOf[T]    
     val outputs = (|| (for (out<-outs) yield proc {out!v}))
     repeat { v = in?; outputs() }
    (proc { in.closein} || || (for (out<-outs) yield proc {out.closeout}))()
  }
  
  /**
  Merge several input streams Into a single output stream. Terminate when
  the output stream, or <i>all</i> the input streams have closed.
  {{{           
     >---->|\  out
   ins :   | }-----> 
     >---->|/    
  }}} 
  */
  def merge[T](ins: Seq[?[T]], out: ![T]) = proc
  { 
    serve ( for (in <- ins) yield in ==> { x => out!x } )
   
    (proc {out.closeout} || || (for (in <- ins) yield proc {in.closein}))()
  }
  
  /**
  Repeatedly input pairs of values `(l, r)` from 'lin`, and `rin' and 
  send `f(l, r)` to `out`. 
  */
  def zipwith[L,R,O](f: (L, R) => O)(lin: ?[L], rin: ?[R], out: ![O]) = 
  proc
  { var l     = null.asInstanceOf[L]
    var r     = null.asInstanceOf[R]
    val input = proc { l = lin? } || proc { r = rin? }
    
    repeat { input(); out!f(l, r) }
    
    (lin.closein || rin.closein || out.closeout)()
  }

  /**
  Turns a pair of streams into a stream of pairs.  
  */   
  def zip[L,R](lin: ?[L], rin: ?[R], out: ![(L,R)]) = 
  proc
  { var l = null.asInstanceOf[L]
    var r = null.asInstanceOf[R]
    val doInputs = proc { l = lin? } || proc { r = rin? }
    
    repeat { doInputs(); out!(l, r) }
    
    (lin.closein || rin.closein || out.closeout)()
  }
  
  /**
  Output all the given 'ts' onto the
  output port, then terminate.
  {{{
    +------------+ t1, ...
    | t1, ... tn +---------> 
    +------------+           
  }}} 
  */   
  def const[T](ts: T*)(out: ![T]) = proc
  { for (t<-ts) out!t; out.closeout }
  
  /**
  A composite component that sums its input stream onto its output stream
  {{{            

                  in                        out
   [x,y,z,...] >------>|\              /|---------> [x,x+y,x+y+z,...]
                       |+}----------->{ |
                   +-->|/              \|--+
                   |                       |
                   +----------<{(0)}<------+
  }}}  
  */   
  def Integrator(in: ?[Long], out: ![Long]) = 
  { val mid, back, addl = OneOne[Long]
    (  zipwith ((x: Long, y: Long)=>x+y) (in, addl, mid)
    || tee (mid, List(out, back))
    || prefix(0l)(back, addl)
    )
  }
  
  /**
  Merges the streams `in` and `inj` onto `out`, giving priority
  to data arriving on `inj`.   
  */   
  def inj[T](in: ?[T], inj: ?[T], out: ![T]) = proc
  { 
    priserve ( inj ==> { x => out!x } | in ==> { x => out!x } )
    (out.closeout || in.closein || inj.closein)()
  }
  
  /**
  Repeatedly reads pairs inputs from its two input ports and 
  outputs them (in parallel, and ordered) to its two output ports. 
  {{{            
    x, ...--->[\/]---> max(x,y), ...
    y, ...--->[/\]---> min(x,y), ...
  }}}
  Here is
  a four-channel sorting network composed of 5 such components.
  {{{           
    -->[\/]--------->[\/]------------>
    -->[/\]---+  +-->[/\]--+
              |  |         |
              |  |         +-->[\/]-->
    -->[\/]------+         +-->[/\]-->
    -->[/\]-+ |            |
            | +---->[\/]---+
            +------>[/\]------------->
  }}}  
  */   
  def exchanger[T <% Ordered[T]](l:  ?[T],  r:  ?[T], lo: ![T],  hi: ![T]) = 
  proc
  { var lv, rv = null.asInstanceOf[T]
    val rdBoth = proc  { lv=l? } || proc  { rv=r? }
    val wrBoth = proc  { lo!rv } || proc  { hi!lv }
    repeat 
    { rdBoth()
      if (lv < rv) { val t = lv; lv=rv; rv=t }
      wrBoth()
    }
    (  l.closein   || r.closein
    || lo.closeout || hi.closeout)()
  }  
  
  /**
  Generate a `?[Unit]` on which an `()` is made available 
  by a server process every `periodMS` milliseconds.
  Terminate the server when the port is closed.
  {{{
   +----------+           
   | periodMS |>-------------> () 
   +----------+           
  }}} 
  */   
  def Ticker(periodMS: Long) : ?[Unit] =
  { import ox.CSO._
    val ticks = OneOne[Unit]
    fork { repeat { ticks!(); sleep(periodMS) } } 
    return ticks
  }
  
  /**
  Drop the first value read from `in`, then copy
  values from `in` to `out`.
    
  */   
  def tail[T](in: ?[T], out: ![T]) = proc { in?; copy(in, out)() }
  
  /**
  Output the given `ts` to `out`, then copy
  values from `in` to `out`.
    
  */   
  def prefix[T](ts:T*)(in: ?[T], out: ![T]) = proc { for (t<-ts) out!t; copy(in, out)() }
  
  /**
  Repeatedly copy values from `in` to `out`.  
  */   
  def copy[T] (in: ?[T], out: ![T]) = proc { repeat { out!(in?) }; out.closeout; in.closein }
  
  /**
  Copy values from `in` to `out` that satisfy `pass`. 
    
  */   
  def filter[T] (pass: T => Boolean) (in: ?[T], out: ![T]) = proc
  { repeat { val v=in?; if (pass(v)) out!v }  
    (out.closeout || in.closein)()
  }    
  
  /**
  {{{ 
     x, ... >-->[f]>-->f(x), ...            
  }}} 
  */   
  def map[I,O] (f: I => O) (in: ?[I], out: ![O]) = proc 
  { repeat { out!(f(in?)) }
    (out.closeout || in.closein)() 
  }
  
  /**
  Repeatedly write the string forms of values read from `in`
  onto the standard output stream.  
  */   
  def console[T](in: ?[T]) = proc { repeat { Console.println(in?) } }
  
  /**
  Repeatedly output lines read from the given <tt>LineNumberReader</tt>.  
  */   
  def lines(in: java.io.LineNumberReader, out: ![String]): PROC = proc
  { repeat 
    { val line = try { in.readLine } catch { case _ => null }
      if (line==null) stop
      out!line
    }
    (out.closeout || in.close)() 
  }
  
  /**
  Repeatedly output lines read from the given <tt>Reader</tt>.  
  */   
  def lines(in: java.io.Reader, out: ![String]): PROC = lines(new java.io.LineNumberReader(in), out)
  
  /**
  Repeatedly output lines read from the standard input stream.  
  */   
  def keyboard(out: ![String]) = lines(new java.io.InputStreamReader(System.in), out)
  
  /**
  Read data from `in` as fast as it appears there, copying the most-recently read datum
  to `out` every `periodMS` milliseconds. 
  */
  def sampler[T](periodMS: Long, in: ?[T], out: ![T]) = proc
  { val ticker = Ticker(periodMS) 
    var datum  = null.asInstanceOf[T]
    priserve ( ticker ==> { case () => out!datum } 
               // Note Scala syntactic eccentricity for ()=>
             | in     ==> { case d  => datum = d } 
             ) 
    (in.closein || out.closeout || ticker.close)()   
  }
}







