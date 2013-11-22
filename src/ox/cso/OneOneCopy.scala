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
  
  A finitely buffered point-to-point Chan implementation with partial
  dynamic enforcement of the point-to-point restriction. Conditions
  that will be detected dynamically and cause an exception to be
  thrown are: more than one output process attempting to write to
  a buffer that is already full, and more than one input process
  attempting to read from an empty buffer.
  
  '''DEPRECATED''': use `OneOneBuf`

  
  
  
  
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
*/


class   OneOneCopy[T: Manifest] (size: Int, id: String) 
extends Chan[T] 
with    InPort.Proxy[T]
with    OutPort.Proxy[T]
with    UnSharedInPort[T] 
with    UnSharedOutPort[T]
{ import ox.CSO._
  private var buf       = new Array[T](size)
  private var front     = 0
  private var rear      = 0
  private var count     = 0
  private var name      = OneOneCopy.genName(id) 
  override def toString = name
  
  def this(size: Int)             = this(size, null)       
  def this(id: String, size: Int) = this(size, id) 
  
  private val left, right = OneOne[T]
  val inport              = right
  val outport             = left
  private val leftport  : ?[T] = left
  private val rightport : ![T] = right
  
  override def close 
  { 
    left.close
    right.close
  }
        
  override def closeout 
  { 
    left.closeout
  }
        
  override def closein 
  { 
    right.closein
  }
        
  val thread = proc 
  { serve( (count>0    &&& rightport)  -->  { val t = buf(front); front=(front+1)%size; count=count-1; right!t }
         | (count<size &&& leftport)   ==>> { case t => buf(rear)=t; rear=(rear+1)%size; count=count+1 }
         )
    close
  } . fork
  
}

object OneOneCopy extends NameGenerator("OneOneCopy-")







