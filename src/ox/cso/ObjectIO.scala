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


package ox.cso

import ox.CSO._
import java.io.EOFException
import java.net.SocketException

/**
These utilities provide a low-level Interface between cso ports
and serialized object input/output.
  
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 553 $ 
 $Date: 2012-08-25 13:22:48 +0100 (Sat, 25 Aug 2012) $
}}}  
*/
object ObjectIO
{  
   /** 
      The Scala types that can be dealt with by the port-copying machinery.
   */
   type Serial = Any

   /** A process that copies objects of type <tt>T</tt> from <tt>in</tt> to <tt>out</tt> */
   def StreamToPort[T <: Serial](in: java.io.InputStream, out: OutPort[T]): PROC =
   { val objects = new ObjectInput[T](in)
     /* 
        It is not possible to ask whether an ObjectInputStream has
        a complete object available for reading; only to ask how
        many bytes are readable. This means that we cannot directly
        build an implementation of an InPort from an ObjectInputStream.
        For this reason we need to employ a ''transfer thread''
        that can safely commit to input.
     */ 
     proc ("StreamToPort")
     { var open = true
       repeat (open)
       { try 
         { val o = objects.readUnshared.asInstanceOf[T]
           out!o
         }
         catch { case e: EOFException    => open=false 
                 case e: SocketException => open=false
               }
       }
       (proc {out.close} || proc {objects.close})() 
     }
   }

   /** A process that copies objects of type <tt>T</tt> from <tt>in</tt> to <tt>out</tt>.
       If <code>sync</code> is true then the stream is flushed once per object written.
   */   
   def PortToStream[T <: Serial](in: InPort[T], out: java.io.OutputStream, sync: Boolean): PROC =
   { val objects = new ObjectOutput[T](out)
     proc ("PortToStream")
     { repeat { in?( (o:T) => { objects.writeUnshared(o); objects.reset; if (sync) objects.flush } ) }
       (proc {out.close()} || proc {objects.close()})() 
     }
   } 
       
   /** 
     An OutPort that transfers objects to the given OutputStream;
     When sync is true the transfer is immediate.
   */   
   class ObjectOutPort[T <: Serial](out: java.io.OutputStream, sync: Boolean)
   extends OutPort[T]{
     val objects = new ObjectOutput[T](out)
     override def !(value: T) = 
     { objects.writeUnshared(value)
       objects.reset
       if (sync) objects.flush 
     }
     
     // def isWriteable(whenWriteable: ()=>Unit) : Boolean = true
     
     override def close = { objects.flush; objects.close }

     // Operations for alts not currently supported
     def registerOut(a:Alt,n:Int) : Int = 
       throw new UnsupportedOperationException() 
     def deregisterOut(a:Alt, n:Int) = 
       throw new UnsupportedOperationException() 
   } 
   
   /**
      ObjectOutputStream that does not write the class code for objects, but simply writes the class name
   */
   class ObjectOutput[T <: Serial](out: java.io.OutputStream) extends java.io.ObjectOutputStream(out)
   { 
      override def writeClassDescriptor(desc: java.io.ObjectStreamClass) 
               { writeUTF(desc.getName) }
   }  
   
   /**
      Dual of ObjectOutput
   */
   class ObjectInput[T <: Serial](in: java.io.InputStream) extends java.io.ObjectInputStream(in)
   { 
     override def readClassDescriptor() : java.io.ObjectStreamClass =
              { java.io.ObjectStreamClass.lookup(Class.forName(readUTF)) }     
   }   
}















