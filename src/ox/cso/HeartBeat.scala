/*

Copyright © 2007, 2008 Bernard Sufrin, Worcester College, Oxford University

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

/**
This module provides the simplest possible implementation of
adapters that can be used to transform an ordinary buffer Into
a ''heartbeat'' buffer. The receiving end of such a buffer is
capable of discovering that the sending end is no longer (capable
of being) in contact with it.

The method is simple: the transmitting end guarantees to send
something along the channel periodically: either a ''ping''
message or a message containing real data. If the receiving
end doesn't receive anything for (slightly more than) one period,
then it decides that the sending end has ''gone down''. 
        
{{{
 @version 03.20120824
 @author Bernard Sufrin, Oxford
 $Revision: 623 $ 
 $Date: 2013-02-12 18:36:06 +0000 (Tue, 12 Feb 2013) $
}}}
*/
object HeartBeat
{ trait Message
  case  object  Ping             extends Message {}
  case  class   Data[T](data: T) extends Message {}
  
  /**
  Copies data from `from` to `to` in `Data` packets, 
  padding the `Message` stream out with `Ping` so that
  it sends at least one packet every `pulse` ms.
  */
  def transmitter[T](pulse: Long, from: ?[T], to: ![Message]) = 
  proc 
  { serve( from         ==> { case t => to!Data(t) }
         | after(pulse) ==> { to!Ping }
         ) 
    from.closein
    to.closeout
  }
  /**
  Copies the data from the `Data` packets arriving on `from` to `to`,
  expecting at least one `Message` packet every `pulse` ms, and sending
  a message to `fail` if the `from` port doesn't comply.
  */
  def receiver[T](pulse: Long, from: ?[Message], to: ![T], fail: ![Unit]) = 
  proc 
  { serve( from ==>
           { case Ping        => ()
             case d : Data[T] => to!d.data 
             // the above is a circumlocution to avoid erasure warning
             // WAS case Data(d:T) => to!d

           }
         | after(pulse) ==> { fail!() }
         ) 
    from.closein
    to.closeout 
    fail.closeout 
  }
}







