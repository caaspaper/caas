package de.uni_stuttgart.caas.admin.messages;

import java.util.Collection;
import delaunay.Segment;

/**
 * Message sent to cache node containing information about neighboring nodes
 */
public class AddToGridMessage implements IMessage{

	private Collection<Segment> neighoringNodes;

	/**
	 * Constructor of message
	 * 
	 * @param neighboringNodes
	 *            a collection of neighboring nodes
	 */
	public AddToGridMessage(Collection<Segment> neighboringNodes) {
		this.neighoringNodes = neighboringNodes;
	}

	/**
	 * Get the neighboring nodes
	 * @return the neighboring nodes
	 */
	public Collection<Segment> getNeighboringNodes() {
		return neighoringNodes;
	}

	@Override
	public MessageType GetMessage() {
		return MessageType.ADD_TO_GRID;
	}
}
