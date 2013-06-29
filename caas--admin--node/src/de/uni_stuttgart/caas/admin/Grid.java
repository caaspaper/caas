package de.uni_stuttgart.caas.admin;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public DelaunayTriangulation triangulation;

	/**
	 * Set containing all connected Nodes
	 */
	public Map<InetSocketAddress, NodeInfo> connectedNodes;

	public Grid(JoinRequestManager joinRequests) {
		assert joinRequests.IsComplete();
		// TODO construct grid based on join requests

	}

	/**
	 * Called once to add all the nodes from the join request manager to the
	 * hashmap assigning them a location on the grid
	 * 
	 */
	private void distributeCacheNodesOnGrid() {

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
	private void addNodeToTriangulation(Point p) {
		if (triangulation == null) {
			triangulation = new Knuth();
		}
		triangulation.addPoint(p);
		triangulation.update();
	}

	public Collection<NodeInfo> getNeighborInfo(InetSocketAddress addressOfNode) {
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