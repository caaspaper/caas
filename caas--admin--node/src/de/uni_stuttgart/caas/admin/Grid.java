package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.NodeInfo;
import delaunay_triangulation.Delaunay_Triangulation;
import delaunay_triangulation.Point_dt;
import delaunay_triangulation.Triangle_dt;

/**
 * 
 *
 */
public class Grid {

	/**
	 * Provides access to triangulation
	 */
	private Triangulation triangulation;

	/**
	 * Set containing all connected Nodes
	 */
	private Map<InetSocketAddress, NodeInfo> connectedNodes;

	private Map<LocationOfNode, InetSocketAddress> pointToAddressMapping;
	
	/**
	 * bounds for the grid
	 */
	public static final int MAX_GRID_INDEX = 1024;
	public static final int MIN_GRID_INDEX = 0;

	/**
	 * number of connections
	 */
	private int numOfConnectedNodes = 0;

	/**
	 * Construct a new Grid using a joinRequestManager
	 * 
	 * @param joinRequests
	 */
	public Grid(JoinRequestManager joinRequests) {
		assert joinRequests.IsComplete();

		numOfConnectedNodes = joinRequests.getNumberOfConnectedNodes();
		connectedNodes = new HashMap<>(numOfConnectedNodes);
		pointToAddressMapping = new HashMap<>();
		distributeCacheNodesOnGrid(joinRequests.getAddressesOfConnectedNodes());

		performNewTriangulation();
	}

	
	public Delaunay_Triangulation getTriangulation() {
		return triangulation;
	}
	/**
	 * Called once to add all the nodes from the join request manager to the
	 * hashmap assigning them a location on the grid
	 * 
	 */
	private void distributeCacheNodesOnGrid(Queue<InetSocketAddress> addresses) {

		LocationOfNode currentPoint = new LocationOfNode(0, 0);
		
		for (InetSocketAddress addr : addresses) {
			while(pointToAddressMapping.containsKey(currentPoint = new LocationOfNode(Math.random() * MAX_GRID_INDEX, Math.random() * MAX_GRID_INDEX))) {}
			connectedNodes.put(addr, new NodeInfo(addr, currentPoint));
			pointToAddressMapping.put(currentPoint, addr);
		}
		
	}

	/**
	 * perform a triangulation on the nodes currently in the hashmap note:
	 * clears the previous triangulation
	 */
	private void performNewTriangulation() {
		
		triangulation = new Triangulation(pointToAddressMapping.keySet().toArray(new Point_dt[0]));
	}

	/**
	 * Add a new point and update triangulation
	 * 
	 * @param p
	 *            the point to add
	 */
	private void addPointToTriangulation(LocationOfNode p) {
		if (triangulation == null) {
			triangulation = new Triangulation();
		}
		triangulation.addPoint(p);
	}

	/**
	 * Adds node to triangulation choosing a random point
	 * 
	 * @param address
	 *            the address of the node
	 * @throws IllegalArgumentException
	 *             if the address is already
	 */
	public void addNewNode(InetSocketAddress address) {

		if (connectedNodes.containsKey(address)) {
			throw new IllegalArgumentException("node already in triangulation");
		}
		LocationOfNode currentPoint = null;
		while(pointToAddressMapping.containsKey(currentPoint = new LocationOfNode(Math.random() * MAX_GRID_INDEX, Math.random() * MAX_GRID_INDEX))) {}
		addNewNode(address, currentPoint);
	}

	/**
	 * Adds node to triangualtion given a point
	 * 
	 * @param address
	 *            the address of the node
	 * @param p
	 *            the location of the point
	 * @throws IllegalArgumentException
	 *             if the address or point are already in triangulation
	 */
	public void addNewNode(InetSocketAddress address, LocationOfNode p) {

		if (connectedNodes.containsKey(address) || pointToAddressMapping.containsKey(p)) {
			throw new IllegalArgumentException("Address or location of node are already in triangulation");
		}

		pointToAddressMapping.put(p, address);
		connectedNodes.put(address, new NodeInfo(address, p));
		addPointToTriangulation(p);
	}

	/**
	 * update location of a node
	 * 
	 * @param address
	 *            the address of the node
	 * @param newLocation
	 *            the new location of the node
	 */
	public void updateLocationOfNode(InetSocketAddress address, LocationOfNode newLocation) {
		LocationOfNode oldLocation = connectedNodes.get(address).getLocationOfNode();
		connectedNodes.get(address).updateLocation(newLocation);
		pointToAddressMapping.remove(oldLocation);
		pointToAddressMapping.put(newLocation, address);
		triangulation.updatePoint(oldLocation, newLocation);
	}

	/**
	 * Return a collection of all neighbors given an address
	 * 
	 * @param addressOfNode
	 *            the address of the node
	 * @return a collection with NodeInfos about the neighbors
	 */
	public Collection<NodeInfo> getNeighborInfo(InetSocketAddress addressOfNode) {
		
		
		Collection<NodeInfo> infoOnNeighbors = new ArrayList<>();
		LocationOfNode pointOfNode = connectedNodes.get(addressOfNode).getLocationOfNode();

		for (LocationOfNode p : triangulation.getNeighbors(pointOfNode)) {
			
			infoOnNeighbors.add(new NodeInfo(pointToAddressMapping.get(p), p));
		}
		return infoOnNeighbors;
	}

	/**
	 * Method to get Location of node given it's address
	 * 
	 * @param addr
	 *            the address of the node
	 * @return the location of the node
	 */
	public LocationOfNode getLocationOfNode(InetSocketAddress addr) {
		NodeInfo l = connectedNodes.get(addr);
		if (l == null) {
			throw new IllegalArgumentException("Address not in Grid");
		}
		return l.getLocationOfNode();
	}
	
	private class Triangulation extends Delaunay_Triangulation {
		
		private Map<LocationOfNode, ArrayList<LocationOfNode>> neigbors;
		
		public Triangulation(Point_dt[] array) {
			super(array);
			neigbors = new HashMap<>();
			updateNeighbors();
		}

		public Triangulation() {
			super();
			neigbors = new HashMap<>();
		}

		public ArrayList<LocationOfNode> getNeighbors(LocationOfNode l) {
			
			if (neigbors.size() == 0) {
				throw new IllegalStateException("The triangulations hasn't been initialized");
			}
			return neigbors.get(l);
		}
		
		public void updatePoint(LocationOfNode pointToDelete, LocationOfNode newPoint) {
			super.deletePoint(pointToDelete);
			addPoint(newPoint);
			updateNeighbors();
		}
		
		public void removePoint(LocationOfNode pointToDelete) {
			super.deletePoint(pointToDelete);
			updateNeighbors();
		}
		
		public void addPoint(LocationOfNode p) {
			super.insertPoint(p);
			updateNeighbors();
		}
		
		private void updateNeighbors() {
			neigbors.clear();
			Iterator<Triangle_dt> triangles = trianglesIterator();
			Triangle_dt current = null;
			while (triangles.hasNext()) {
				current = triangles.next();
				addNeighbor((LocationOfNode)current.p1(), (LocationOfNode)current.p2());
				addNeighbor((LocationOfNode)current.p1(), (LocationOfNode)current.p3());
				addNeighbor((LocationOfNode)current.p2(), (LocationOfNode)current.p1());
				addNeighbor((LocationOfNode)current.p2(), (LocationOfNode)current.p3());
				addNeighbor((LocationOfNode)current.p3(), (LocationOfNode)current.p1());
				addNeighbor((LocationOfNode)current.p3(), (LocationOfNode)current.p2());
			}
			System.out.println("done");
		}
		
		private void addNeighbor(LocationOfNode point, LocationOfNode neighbor) {
			ArrayList<LocationOfNode> tempList = neigbors.get(point);
			if (tempList == null) {
				tempList = new ArrayList<>();
				neigbors.put(point, tempList);
			}
			if (!tempList.contains(neighbor)) {
				tempList.add(neighbor);
			}
			
		}
	}
	
}
