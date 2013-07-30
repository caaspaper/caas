package de.uni_stuttgart.caas.base;

import java.io.Serializable;

import delaunay_triangulation.Point_dt;

public class LocationOfNode extends Point_dt implements Serializable {

	public final int x, y;
	
	public LocationOfNode (double x, double y) {
		super((int)x, (int)y);
		this.x = (int) x;
		this.y = (int) y;
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}
