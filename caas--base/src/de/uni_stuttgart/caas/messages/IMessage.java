package de.uni_stuttgart.caas.messages;

import java.io.Serializable;

public interface IMessage extends Serializable {

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
		CONFIRM,

		/**
		 * Message containing query information and maybe even the actual query
		 * from the client
		 */
		QUERY_MESSAGE,

		/**
		 * Message containing the result of a query
		 */
		QUERY_RESULT,

		/**
		 * Used when establishing neighbor connections to allow nodes to
		 * identify themselves. 
		 */
		PUBLISH_ID,
		
		/**
		 * used to exchange information about load of a node
		 */
		LOAD_MESSAGE
	}

	/**
	 * Get the type of the message
	 * 
	 * @return The type of the message
	 */
	MessageType getMessageType();

}
