package de.uni_stuttgart.caas.cache;

import java.util.TreeSet;
import de.uni_stuttgart.caas.admin.messages.ConfirmationMessage;
import de.uni_stuttgart.caas.admin.messages.IMessage;
import de.uni_stuttgart.caas.admin.messages.IMessage.MessageType;
import de.uni_stuttgart.caas.admin.messages.JoinMessage;
import delaunay.Point;

/**
 * Class representing the cache node
 */
public class CacheNode {

	/**
	 * reference to the actual cache TODO
	 */

	/**
	 * position of this node
	 */
	private Point position;

	/**
	 * reference rest of data TODO
	 */

	/**
	 * neighboring nodes
	 */
	private TreeSet<NodeInfo> neighboringNodes;

	/**
	 * current state
	 */
	private CacheNodeState currentState;

	/**
	 * initialize node and generate JOIN-Message
	 * 
	 * @return The newly generated JOIN-Message
	 */
	public IMessage initializeNode() {

		currentState = CacheNodeState.AWAITING_DATA;
		return new JoinMessage();
	}

	/**
	 * Add a new neighboring node
	 * 
	 * @param newNode
	 *            The new node to add
	 */
	private void addNeighbor(NodeInfo newNode) {
		if (!neighboringNodes.contains(newNode)) {
			neighboringNodes.add(newNode);
		}
	}

	/**
	 * Remove a neighboring node
	 * 
	 * @param node
	 *            The node to remove
	 */
	private void removeNeighbor(NodeInfo node) {
		neighboringNodes.remove(node);
	}

	public IMessage process(IMessage message) {

		MessageType type = message.GetMessage();
		switch (currentState) {

		case AWAITING_ACTIVATION:

			if (type != MessageType.ACTIVATE) {

			}
			currentState = CacheNodeState.ACTIVE;
			break;

		case AWAITING_DATA:

			if (type != MessageType.ADD_TO_GRID) {

			}
			currentState = CacheNodeState.AWAITING_ACTIVATION;
			break;

		case ACTIVE:

			break;

		default:
			break;
		}
		
		// TODO change default response
		return new ConfirmationMessage(-1, "error"); 
	}

}
