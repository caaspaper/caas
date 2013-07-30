///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [NodeInfo.java]
// (c) 2013 Ashley Marie Smith, Simon Hanna, Alexander Gessler
//
// All rights reserved.
//
// This code may not be published, distributed or otherwise made available to
// third parties without the prior written consent of the copyright owners.
//
///////////////////////////////////////////////////////////////////////////////////

package de.uni_stuttgart.caas.base;

import java.io.Serializable;
import java.net.InetSocketAddress;
import delaunay.Point;

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
	private Point locationOfNode;

	/**
	 * Construct a new NodeInfo given the address of the node
	 * 
	 * @param nodeAdress
	 *            The address of the node
	 */
	public NodeInfo(InetSocketAddress nodeAdress) {
		NODE_ADDRESS = nodeAdress;
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
	public NodeInfo(InetSocketAddress nodeAdress, Point locationOfNode) {
		this(nodeAdress);
		updateLocation(locationOfNode);
	}

	/**
	 * Update the location of a node
	 * 
	 * @param locationOfNode
	 *            the new location of the node
	 */
	public void updateLocation(Point locationOfNode) {
		this.locationOfNode = locationOfNode;
	}

	/**
	 * Get the location of the node
	 * 
	 * @return The location of the node
	 */
	public Point getLocationOfNode() {
		return locationOfNode;
	}

	@Override
	public int hashCode() {
		return NODE_ADDRESS.hashCode();

	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof NodeInfo) {
			return this.NODE_ADDRESS == ((NodeInfo) o).NODE_ADDRESS;
		}
		return false;

	}
	
	@Override
	public String toString() {
		return NODE_ADDRESS + "->" + locationOfNode;
	}

}

/* vi: set shiftwidth=4 tabstop=4: */ 
