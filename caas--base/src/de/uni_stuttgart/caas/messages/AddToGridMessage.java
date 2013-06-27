package de.uni_stuttgart.caas.messages;

import java.util.Collection;

import de.uni_stuttgart.caas.base.NodeInfo;
import delaunay.Segment;

/**
 * Message sent to cache node containing information about neighboring nodes
 */
public class AddToGridMessage implements IMessage{

	private Collection<NodeInfo> neighoringNodes;

	/**
	 * Constructor of message
	 * 
	 * @param neighboringNodes
	 *            a collection of neighboring nodes
	 */
	public AddToGridMessage(Collection<NodeInfo> neighboringNodes) {
		this.neighoringNodes = neighboringNodes;
	}

	/**
	 * Get the neighboring nodes
	 * @return the neighboring nodes
	 */
	public Collection<NodeInfo> getNeighboringNodes() {
		return neighoringNodes;
	}

	@Override
	public MessageType GetMessage() {
		return MessageType.ADD_TO_GRID;
	}
}
