package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import de.uni_stuttgart.caas.admin.JoinRequestManager.JoinRequest;
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
	public static final int MAX_GRID_INDEX = 2000000000;

	
	public static LocationOfNode RandomPoint() {
		return new LocationOfNode((int) (Math.random() * MAX_GRID_INDEX), (int) (Math.random() * MAX_GRID_INDEX));
	}
	
	
	public static LocationOfNode CenterPoint() {
		return new LocationOfNode(MAX_GRID_INDEX / 2, MAX_GRID_INDEX / 2);
	}

	
	private static final Random r = new Random();
	
	public static LocationOfNode SampleGaussian(LocationOfNode center, double stddev) {
		final double x = r.nextGaussian();
		final double y = r.nextGaussian();
		
		return new LocationOfNode((int)(center.x - x * stddev * MAX_GRID_INDEX), (int)(center.y - y * stddev * MAX_GRID_INDEX));
	}

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
		distributeCacheNodesOnGrid(joinRequests.getJoinRequests());

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
	private void distributeCacheNodesOnGrid(List<JoinRequest> joinRequests) {

		LocationOfNode currentPoint = new LocationOfNode(0, 0);

		for (JoinRequest j : joinRequests) {
			while (pointToAddressMapping.containsKey(currentPoint = RandomPoint())) {
			}
			connectedNodes.put(j.ADDRESS, new NodeInfo(j.ADDRESS, currentPoint, j.NEIGHBORCONNECTOR_ADDRESS, j.QUERYLISTENER_ADDRESS, j.ID));
			pointToAddressMapping.put(currentPoint, j.ADDRESS);
		}
	}

	/**
	 * perform a triangulation on the nodes currently in the hashmap note:
	 * clears the previous triangulation
	 */
	private void performNewTriangulation() {

		triangulation = new Triangulation(pointToAddressMapping.keySet());
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
	public void addNewNode(InetSocketAddress address, InetSocketAddress neighborConnectorAddress, InetSocketAddress queryListenerAddress, long id) {

		if (connectedNodes.containsKey(address)) {
			throw new IllegalArgumentException("node already in triangulation");
		}
		LocationOfNode currentPoint = null;
		while (pointToAddressMapping.containsKey(currentPoint = new LocationOfNode((int) (Math.random() * MAX_GRID_INDEX),
				(int) (Math.random() * MAX_GRID_INDEX)))) {
		}
		addNewNode(address, currentPoint, neighborConnectorAddress, queryListenerAddress, id);
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
	public void addNewNode(InetSocketAddress address, LocationOfNode p, InetSocketAddress neighborConnectorAddress, InetSocketAddress queryListenerAddress,
			long id) {

		if (connectedNodes.containsKey(address) || pointToAddressMapping.containsKey(p)) {
			throw new IllegalArgumentException("Address or location of node are already in triangulation");
		}

		pointToAddressMapping.put(p, address);
		connectedNodes.put(address, new NodeInfo(address, p, neighborConnectorAddress, queryListenerAddress, id));
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

			InetSocketAddress addr = pointToAddressMapping.get(p);
			if (addr == null) {
				System.out.println("FATAL ERROR, NODE NOT FOUND IN GRID");
				System.exit(-1);
			}
			NodeInfo info = connectedNodes.get(addr);
			if (info == null) {
				System.out.println("FATAL ERROR NODE NOT FOUND IN GRID");
				System.exit(-1);
			}
			infoOnNeighbors.add(info);
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

	/**
	 * Extends the real triangulation to provide methods to access neighbors of
	 * nodes in a more or less efficient way
	 * 
	 */
	private static class Triangulation extends Delaunay_Triangulation {

		private Map<LocationOfNode, ArrayList<LocationOfNode>> neigbors;

		public Triangulation(Set<LocationOfNode> points) {

			super(createTriangulationData(points));
			neigbors = new HashMap<>();
			updateNeighbors();
		}

		public Triangulation() {
			super();
			neigbors = new HashMap<>();
		}

		private static Point_dt[] createTriangulationData(Set<LocationOfNode> points) {
			Point_dt[] data = new Point_dt[points.size()];
			int i = 0;
			for (LocationOfNode p : points) {
				data[i] = generatePointFromLocation(p);
				i++;
			}
			return data;
		}

		private static Point_dt generatePointFromLocation(LocationOfNode l) {
			return new Point_dt(l.x, l.y);
		}

		private static LocationOfNode generateLocationFromPoint(Point_dt p) {
			if (p == null) {
				return null;
			}
			return new LocationOfNode((int) p.x(), (int) p.y());
		}

		public ArrayList<LocationOfNode> getNeighbors(LocationOfNode l) {

			if (neigbors.size() == 0) {
				throw new IllegalStateException("The triangulations hasn't been initialized");
			}
			return neigbors.get(l);
		}

		public void updatePoint(LocationOfNode pointToDelete, LocationOfNode newPoint) {
			super.deletePoint(generatePointFromLocation(pointToDelete));
			addPoint(newPoint);
			updateNeighbors();
		}

		public void removePoint(LocationOfNode pointToDelete) {
			super.deletePoint(generatePointFromLocation(pointToDelete));
			updateNeighbors();
		}

		public void addPoint(LocationOfNode p) {
			super.insertPoint(generatePointFromLocation(p));
			updateNeighbors();
		}

		/**
		 * gets called on every change in the network, so the neighbors are
		 * always accurate
		 */
		private void updateNeighbors() {
			neigbors.clear();
			Iterator<Triangle_dt> triangles = trianglesIterator();
			Triangle_dt current = null;
			while (triangles.hasNext()) {
				current = triangles.next();
				addNeighbor(generateLocationFromPoint(current.p1()), generateLocationFromPoint(current.p2()));
				addNeighbor(generateLocationFromPoint(current.p1()), generateLocationFromPoint(current.p3()));
				addNeighbor(generateLocationFromPoint(current.p2()), generateLocationFromPoint(current.p1()));
				addNeighbor(generateLocationFromPoint(current.p2()), generateLocationFromPoint(current.p3()));
				addNeighbor(generateLocationFromPoint(current.p3()), generateLocationFromPoint(current.p1()));
				addNeighbor(generateLocationFromPoint(current.p3()), generateLocationFromPoint(current.p2()));
			}
		}

		private void addNeighbor(LocationOfNode point, LocationOfNode neighbor) {
			if (point == null || neighbor == null) {
				return;
			}
			ArrayList<LocationOfNode> tempList = neigbors.get(point);
			if (tempList == null) {
				tempList = new ArrayList<>();
				neigbors.put(point, tempList);
			}
			if (!tempList.contains(neighbor)) {
				tempList.add(neighbor);
			}

		}

		public Vector<Triangle_dt> getTriangles() {

			Vector<Triangle_dt> triangles = new Vector<>();
			Iterator<Triangle_dt> it = trianglesIterator();
			while (it.hasNext()) {
				triangles.add(it.next());
			}
			return triangles;
		}
	}

	public Vector<Triangle_dt> getTriangles() {
		return triangulation.getTriangles();
	}

	public Map<InetSocketAddress, NodeInfo> getConnectedNodes() {
		return connectedNodes;
	}
}
