package de.uni_stuttgart.caas.messages;

import java.util.Collection;

import de.uni_stuttgart.caas.base.NodeInfo;

/**
 * Message sent to cache node containing information about neighboring nodes
 */
public class AddToGridMessage implements IMessage{

	private Collection<NodeInfo> neighboringNodes;

	/**
	 * Constructor of message
	 * 
	 * @param neighboringNodes
	 *            a collection of neighboring nodes
	 */
	public AddToGridMessage(Collection<NodeInfo> neighboringNodes) {
		this.neighboringNodes = neighboringNodes;
	}

	/**
	 * Get the neighboring nodes
	 * @return the neighboring nodes
	 */
	public Collection<NodeInfo> getNeighboringNodes() {
		return neighboringNodes;
	}

	@Override
	public MessageType getMessageType() {
		return MessageType.ADD_TO_GRID;
	}
}
