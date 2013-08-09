package de.uni_stuttgart.caas.base;

import java.io.Serializable;

/**
 * The class represents the location of nodes in the network. 
 * It gets sent over the network, it is not used for triangulation.
 */
public class LocationOfNode implements Serializable {

	public final Integer x, y;
	
	public LocationOfNode (int x, int y) {
		this.x =  x;
		this.y =  y;
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}
	
	
	/* since we are not using this class for the triangulation, 
	 * we need to overwrite hashCode() and equals() 
	 * so that two locations constructed with the same parameters are the same.
	 */
	
	@Override
	public int hashCode() {
		return x.hashCode() + y.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof LocationOfNode) {
			return ((LocationOfNode) o).x.equals(x) && ((LocationOfNode) o).y.equals(y);
		}
		return false;
	}

}
