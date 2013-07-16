package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import de.uni_stuttgart.caas.base.LocationOfNode;
import de.uni_stuttgart.caas.base.NodeInfo;
import delaunay.DelaunayTriangulation;
import delaunay.Knuth;
import delaunay.Point;
import delaunay.Segment;

/**
 * 
 *
 */
public class Grid {

	/**
	 * Provides access to triangulation
	 */
	private DelaunayTriangulation triangulation;

	/**
	 * Set containing all connected Nodes
	 */
	private Map<InetSocketAddress, NodeInfo> connectedNodes;

	private Map<Point, InetSocketAddress> pointToAddressMapping;

	/**
	 * bounds for the grid
	 */
	public static final int MAX_GRID_INDEX = 1000000;
	public static final int MIN_GRID_INDEX = -1000000;

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

	/**
	 * Called once to add all the nodes from the join request manager to the
	 * hashmap assigning them a location on the grid
	 * 
	 */
	private void distributeCacheNodesOnGrid(Queue<InetSocketAddress> addresses) {

		int numberOfNodesInOneLine = (int) Math.ceil(Math.sqrt(addresses.size()));
		int distanceBetweenNodes = (MAX_GRID_INDEX - MIN_GRID_INDEX) / numberOfNodesInOneLine;
		int x = MIN_GRID_INDEX, y = MIN_GRID_INDEX;

		InetSocketAddress currentAddress = null;
		Point currentPoint = new LocationOfNode(0, 0);

		// shell of grid
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			currentPoint = new LocationOfNode(x, y);
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, currentPoint));
			pointToAddressMapping.put(currentPoint, currentAddress);
			x += distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			currentPoint = new LocationOfNode(x, y);
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, currentPoint));
			pointToAddressMapping.put(currentPoint, currentAddress);
			y += distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			currentPoint = new LocationOfNode(x, y);
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, currentPoint));
			pointToAddressMapping.put(currentPoint, currentAddress);
			x -= distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			currentPoint = new LocationOfNode(x, y);
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, currentPoint));
			pointToAddressMapping.put(currentPoint, currentAddress);
			y -= distanceBetweenNodes;
		}
		x += distanceBetweenNodes;
		y += distanceBetweenNodes;

		// fill the rest
		while ((currentAddress = addresses.poll()) != null) {
			currentPoint = new LocationOfNode(x, y);
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, currentPoint));
			pointToAddressMapping.put(currentPoint, currentAddress);
			x += distanceBetweenNodes;
			if (x == MAX_GRID_INDEX) {
				x = MIN_GRID_INDEX + distanceBetweenNodes;
				y += distanceBetweenNodes;
			}
		}
	}

	/**
	 * perform a triangulation on the nodes currently in the hashmap note:
	 * clears the previous triangulation
	 */
	private void performNewTriangulation() {
		Set<InetSocketAddress> keys = connectedNodes.keySet();
		List<Point> points = new ArrayList<>();
		for (InetSocketAddress k : keys) {
			points.add(connectedNodes.get(k).getLocationOfNode());
		}
		triangulation = new Knuth(points);
		triangulation.update();
	}

	/**
	 * Add a new point and update triangulation
	 * 
	 * @param p
	 *            the point to add
	 */
	private void addPointToTriangulation(Point p) {
		if (triangulation == null) {
			triangulation = new Knuth();
		}
		triangulation.addPoint(p);
		triangulation.update();
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
		Point p = new LocationOfNode(Math.rint(MAX_GRID_INDEX), Math.rint(MAX_GRID_INDEX));

		while (pointToAddressMapping.containsKey(p)) {
			p.setLocation(Math.rint(MAX_GRID_INDEX), Math.rint(MAX_GRID_INDEX));
		}
		addNewNode(address, p);
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
	public void addNewNode(InetSocketAddress address, Point p) {

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
	public void updateLocationOfNode(InetSocketAddress address, Point newLocation) {
		Point oldLocation = connectedNodes.get(address).getLocationOfNode();
		connectedNodes.get(address).updateLocation(newLocation);
		pointToAddressMapping.remove(oldLocation);
		pointToAddressMapping.put(newLocation, address);
		performNewTriangulation();
	}

	/**
	 * Return a collection of all neighbors given an address
	 * 
	 * @param addressOfNode
	 *            the address of the node
	 * @return a collection with NodeInfos about the neighbors
	 */
	public Collection<NodeInfo> getNeighborInfo(InetSocketAddress addressOfNode) {
		if (!triangulation.isUpdated()) {
			triangulation.update();
		}
		Collection<Segment> segments = triangulation.getSegments();
		Collection<NodeInfo> infoOnNeighbors = new ArrayList<>();
		Point pointOfNode = connectedNodes.get(addressOfNode).getLocationOfNode();

		for (Segment s : segments) {
			List<Point> pointsInSegment = s.getPoints();

			if (pointsInSegment.get(0).equals(pointOfNode)) {
				infoOnNeighbors.add(new NodeInfo(pointToAddressMapping.get(pointsInSegment.get(1)), pointsInSegment.get(1)));
			} else {
				infoOnNeighbors.add(new NodeInfo(pointToAddressMapping.get(pointsInSegment.get(0)), pointsInSegment.get(0)));
			}
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
	public Point getLocationOfNode(InetSocketAddress addr) {
		NodeInfo l = connectedNodes.get(addr);
		if (l == null) {
			throw new IllegalArgumentException("Address not in Grid");
		}
		return l.getLocationOfNode();
	}
}
