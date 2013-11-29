package de.uni_stuttgart.caas.base;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Class representing information about a node, that is stored by neighboring
 * nodes
 */
public class NodeInfo implements Serializable{

	/**
	 * Address of node
	 */
	public final InetSocketAddress NODE_ADDRESS;

	/**
	 * Location of node
	 */
	private LocationOfNode locationOfNode;
	
	/**
	 * Address of the Socket the node is listening on
	 */
	public final InetSocketAddress ADDRESS_FOR_CACHENODE_NODECONNECTOR;
	
	
	/**
	 * Address of the Socket the node is listening for Queries on
	 */
	public final InetSocketAddress ADDRESS_FOR_CACHENODE_QUERYLISTENER;

	/**
	 * Construct a new NodeInfo given the address of the node
	 * 
	 * @param nodeAdress
	 *            The address of the node
	 */
	public NodeInfo(InetSocketAddress nodeAdress, InetSocketAddress neighborConnectorAddress, InetSocketAddress queryListenerAddress) {
		NODE_ADDRESS = nodeAdress;
		ADDRESS_FOR_CACHENODE_NODECONNECTOR = neighborConnectorAddress;
		ADDRESS_FOR_CACHENODE_QUERYLISTENER = queryListenerAddress;
	}

	/**
	 * Construct a new NodeInfo given the adress of the node, and it's location
	 * in the network
	 * 
	 * @param nodeAdress
	 *            The address of the node
	 * @param locationOfNode
	 *            Location of the node in the network
	 */
	public NodeInfo(InetSocketAddress nodeAdress, LocationOfNode locationOfNode, InetSocketAddress neighborConnectorAddress, InetSocketAddress queryListenerAddress) {
		this(nodeAdress, neighborConnectorAddress, queryListenerAddress);
		updateLocation(locationOfNode);
	}

	/**
	 * Update the location of a node
	 * 
	 * @param locationOfNode
	 *            the new location of the node
	 */
	public void updateLocation(LocationOfNode locationOfNode) {
		this.locationOfNode = locationOfNode;
	}

	/**
	 * Get the location of the node
	 * 
	 * @return The location of the node
	 */
	public LocationOfNode getLocationOfNode() {
		return locationOfNode;
	}

	@Override
	public int hashCode() {
		return NODE_ADDRESS.hashCode();

	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof NodeInfo) {
			return this.NODE_ADDRESS.equals(((NodeInfo) o).NODE_ADDRESS);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return NODE_ADDRESS + "->" + locationOfNode;
	}

}
