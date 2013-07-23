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
