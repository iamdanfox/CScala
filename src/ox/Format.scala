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


package ox
import scala.runtime._
/** 
 Formatted strings using the Java library formatting conventions.
 This was written in 2006, and has probably been superseded by
 something in the standard Scala library by now; but too much of
 my own CSO code for comfort uses it.
 
{{{
 @see java.util.Formatter 
 @author  Bernard Sufrin, Oxford
 @version 03.20120824
 $Revision: 470 $ 
 $Date: 2012-08-25 12:12:44 +0100 (Sat, 25 Aug 2012) $
}}}
 
*/
object Format
{ //import java.lang.SuppressWarnings
  /** Format args to form a string */
  // @SuppressWarnings(Array()) 
  def format(format: String, args: Any*) : String =
      java.lang.String.format(format, boxedArray(args) : _*)
  
  /** Format args as <code>fmt(0) punct fmt(1) punct ... fmt(N)</code> where
      <code>fmt(i)=format(fmt, arg(i))</code>. 
  */
  def format(fmt: String, punct: String, args: Seq[Any]) : String =
  { val s = new StringBuilder
    val e = args.iterator
    while (e.hasNext) 
    { s++=format(fmt, e.next)
      if (e.hasNext) s++=punct
    }
    s.toString
  }
  
  /** Output the formatted args to <code>writer</code>  */  
  def printf(writer: java.io.PrintWriter, format: String, args: Any*) : Unit =
      { writer.printf(format, boxedArray(args) : _*); () }
      
  /** Output the formatted args to <code>stream</code>  */  
  def printf(stream: java.io.PrintStream, format: String, args: Any*)  : Unit=
      { stream.printf(format, boxedArray(args) : _*); () }
  
  /** Output the formatted args to <code>System.err</code>  */  
  def eprintf(format: String, args: Any*) : Unit =
      { System.err.printf(format, boxedArray(args) : _*); () }
  
  /** Output the formatted args to <code>System.out</code>  */  
  def printf(format: String, args: Any*) : Unit =
      { System.out.printf(format, boxedArray(args) : _*); () }
  
  /** Construct an array of boxed objects from a sequence of values. 
      <p>
      I gratefully acknowledge the use of part of the <code>Console</code>
      source for Scala version 2.5.1
  */
  private def boxedArray(s: Seq[Any]): Array[AnyRef] = {
    val res = new Array[AnyRef](s.length);
    var i: Int = 0;
    val iter = s.iterator;
    while (iter.hasNext) {
      res(i) = iter.next match {
        case x: Boolean => new java.lang.Boolean(x)
        case x: Byte => new java.lang.Byte(x)
        case x: Short => new java.lang.Short(x)
        case x: Char => new java.lang.Character(x)
        case x: Int => new java.lang.Integer(x)
        case x: Long => new java.lang.Long(x)
        case x: Float => new java.lang.Float(x)
        case x: Double => new java.lang.Double(x)
        case x: Unit => "()"
        case null => "null"
        case x: AnyRef => x
      }
      i = i + 1
    }
    res
  }
      
}











