///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [AddToGridMessage.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

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

/* vi: set shiftwidth=4 tabstop=4: */ 