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
	
	/**
	 * bounds for the grid
	 */
	public static final int MAX_GRID_INDEX =  1000000;
	public static final int MIN_GRID_INDEX = -1000000;
	
	/**
	 * number of connections
	 */
	private int numOfConnectedNodes = 0;

	/**
	 * 
	 * @param joinRequests
	 */
	public Grid(JoinRequestManager joinRequests) {
		assert joinRequests.IsComplete();
		
		numOfConnectedNodes = joinRequests.getNumberOfConnectedNodes();
		connectedNodes = new HashMap<>(numOfConnectedNodes);
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
		
		// shell of grid
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			assert currentAddress != null : "my math should be correct....";
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, new LocationOfNode(x, y)));
			x += distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			assert currentAddress != null : "my math should be correct....";
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, new LocationOfNode(x, y)));
			y += distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			assert currentAddress != null : "my math should be correct....";
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, new LocationOfNode(x, y)));
			x -= distanceBetweenNodes;
		}
		for (int j = 0; j < numberOfNodesInOneLine; j++) {
			currentAddress = addresses.poll();
			assert currentAddress != null : "my math should be correct....";
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, new LocationOfNode(x, y)));
			y -= distanceBetweenNodes;
		}
		x += distanceBetweenNodes;
		y += distanceBetweenNodes;
		
		// fill the rest
		while ((currentAddress = addresses.poll()) != null) {
			connectedNodes.put(currentAddress, new NodeInfo(currentAddress, new LocationOfNode(x, y)));
			x += distanceBetweenNodes;
			if (x == MAX_GRID_INDEX) {
				x = MIN_GRID_INDEX + distanceBetweenNodes;
				y += distanceBetweenNodes;
			}
		}
	}

	/**
	 * perform a triangulation on the nodes currently in the hashmap
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
	 * Adds a point to the triangulation and updates the triangulation
	 * 
	 * @param p
	 *            the point to add
	 */
	public void addNodeToTriangulation(Point p) {
		if (triangulation == null) {
			triangulation = new Knuth();
		}
		triangulation.addPoint(p);
		triangulation.update();
	}

	public Collection<NodeInfo> getNeighborInfo(InetSocketAddress addressOfNode) {
		if (!triangulation.isUpdated()) {
			triangulation.update();
		}
		Collection<Segment> segments = triangulation.getSegments();
		Collection<NodeInfo> infoOnNeighbors = new ArrayList<>();
		Point pointOfNode = connectedNodes.get(addressOfNode)
				.getLocationOfNode();

		for (Segment s : segments) {
			List<Point> pointsInSegment = s.getPoints();

			// TODO Add the address of the point, we need a mapping for that
			if (pointsInSegment.get(0).equals(pointOfNode)) {
				infoOnNeighbors.add(new NodeInfo(null, pointsInSegment.get(1)));
			} else {
				infoOnNeighbors.add(new NodeInfo(null, pointsInSegment.get(0)));
			}
		}
		return infoOnNeighbors;
	}
}
