package de.uni_stuttgart.caas.messages;

public interface IMessage {

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
	MessageType GetMessage();

}
