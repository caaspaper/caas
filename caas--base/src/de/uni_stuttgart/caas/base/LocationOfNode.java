package de.uni_stuttgart.caas.base;

import java.io.Serializable;

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
