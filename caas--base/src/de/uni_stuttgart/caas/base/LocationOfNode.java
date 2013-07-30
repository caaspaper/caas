///////////////////////////////////////////////////////////////////////////////////
// Cache as a Service (projekt-inf) (v0.1)
// [LocationOfNode.java]
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

import delaunay.Point;

public class LocationOfNode implements Point, Serializable {

	private double x;
	private double y;
	
	
	@Override
	public int getIX() {
		return (int) x;
	}

	@Override
	public int getIY() {;
		return (int) y;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public void setLocation(double x, double y) {
		
		this.x = x;
		this.y = y;

	}
	
	public LocationOfNode (double x, double y) {
		setLocation(x, y);
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + y + ")";
	}

}

/* vi: set shiftwidth=4 tabstop=4: */ 
