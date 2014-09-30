package org.fruct.oss.socialnavigator.utils;

import org.osmdroid.util.GeoPoint;

public class Turn {
	private GeoPoint geoPoint;
	private int turnSharpness;
	private int turnDirection;

	public Turn(GeoPoint geoPoint, int turnSharpness, int turnDirection) {

		this.geoPoint = geoPoint;
		this.turnSharpness = turnSharpness;
		this.turnDirection = turnDirection;
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public int getTurnSharpness() {
		return turnSharpness;
	}

	public int getTurnDirection() {
		return turnDirection;
	}
}
