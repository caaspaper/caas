///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [IMessage.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.messages;

import java.io.Serializable;

public interface IMessage extends Serializable{

	public enum MessageType {

		/**
		 * sent to admin node by a node wishing to join the grid. Empty
		 * messages, admin records source ip+port and sends back: OK if state is
		 */
		JOIN,

		/**
		 * sent to a cache node to inform it about its spot in the grid,
		 * contains a 2D position, initial cache radius and list of neighbor
		 * cache nodes identified by (ip+port). Expect response: OK
		 */
		ADD_TO_GRID,

		/**
		 * sent to a cache node to activate it. Upon activation, a cache node
		 * starts processing client requests.
		 */
		ACTIVATE,

		/**
		 * sent to confirm another message or an action
		 */
		CONFIRM
	}

	/**
	 * Get the type of the message
	 * 
	 * @return The type of the message
	 */
	MessageType getMessageType();

}

/* vi: set shiftwidth=4 tabstop=4: */ 