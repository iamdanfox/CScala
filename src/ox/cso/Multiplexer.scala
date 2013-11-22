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
import  collection.mutable.HashMap

/**

        A Multiplexer (more precisely a Connection Multiplexer)
        $Id: Multiplexer.scala 553 2012-08-25 12:22:48Z sufrin $



object Multiplexer
{ class Message {}
  case  class open(name: String, chan: Int);
  case  class close(name: String);
  case  class traffic(chan: Int, obj: AnyRef);
  
  def server
  (services: Connection.Client[
   client:   Connection.Client[Message, Message]) = proc
  { val channel  = new HashMap[String,int]
    var lastChan = 0  
    alt ( services ==> { 
    
        )
  }
  
  def client(server: Connection.Client[Message, Message]) extends Process
  { val channel  = new HashMap[String,int]
    var lastChan = 0  
    repeat
    {
    }
  }
  
  
}

*/

