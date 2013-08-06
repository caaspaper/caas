package de.uni_stuttgart.caas.messages;

import java.util.Collection;

import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.NodeInfo;
import delaunay.Point;

/**
 * Message sent to cache node containing information about neighboring nodes
 */
public class AddToGridMessage implements IMessage{

	/**
	 * Location of the node, the message is sent to
	 */
	public final LocationOfNode locationOfNode;
	
	/**
	 * Information about neighboring nodes
	 */
	private Collection<NodeInfo> neighboringNodes;

	/**
	 * Constructor of message
	 * 
	 * @param neighboringNodes
	 *            a collection of neighboring nodes
	 */
	public AddToGridMessage(LocationOfNode locationOfNode, Collection<NodeInfo> neighboringNodes) {
		
		this.locationOfNode = locationOfNode;
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
