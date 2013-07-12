///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [DebugListenerSet.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.base;

import java.util.ArrayList;

/** Represents a set of debug listeners and simplifies writing to all 
 *  of them at once by using the write() member.
 *
 *  No null elements may be contained in the collection.
 */
@SuppressWarnings("serial")
public class DebugListenerSet extends ArrayList<IDebugListener> 
{
	/** Write a log message to all listeners.
     *
     *  Semantically this is equivalent to atomically calling write(message)
     *  on all listeners in the set.
     *
     *  The method is not threadsafe with respect to any other (modifying)
     *  method in ArrayList, yet it is threadsafe with respect to itself.
     *
     *  @param message Plain text message, may not be null.
     *
	 */
	public synchronized void write(String message) {

		assert message != null;

		for(IDebugListener listener : this) {
			assert listener != null;
    		listener.write(message);
    	}
   	}
}

/* vi: set shiftwidth=4 tabstop=4: */ 