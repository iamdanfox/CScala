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

package ox.cso;

/** 
        A name generator: generates names for a series of objects; the 
        count component of invented names is incremented WHENEVER a name
        is generated; even a name that doesn't incorporate the count.
*/

class NameGenerator(kind: String)
{ private var occurs = 0
  
  /**
      Return <tt>name</tt> if non-null, else an invented name
      constructed from <tt>kind</tt>.
  */
  def genName(name: String) = 
  { occurs = occurs+1
    if (name==null) (kind+occurs.toString) else name
  }
  
  /**
      Return an invented name constructed from <tt>kind</tt>.
  */
  def newName() = 
  { occurs = occurs+1
    kind+"-"+occurs.toString
  }
  
  /**
      Return an invented name constructed from <tt>kind</tt>.
  */
  def newName(kind: String) = 
  { occurs = occurs+1
    kind+"-"+occurs.toString
  }
}
