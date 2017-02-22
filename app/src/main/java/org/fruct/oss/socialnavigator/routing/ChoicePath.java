package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.points.Point;
import org.osmdroid.util.GeoPoint;

public class ChoicePath {
	private final GHResponse response;
	private final Point[] points;
	private final RoutingType routingType;

	private final GeoPoint sourceLocation;
	private final GeoPoint destinationLocation;

	public ChoicePath(GHResponse response, RoutingType routingType, Point[] points,
					  GeoPoint sourceLocation, GeoPoint destinationLocation) {
		this.response = response;
		this.routingType = routingType;
		this.points = points;
		this.sourceLocation = sourceLocation;
		this.destinationLocation = destinationLocation;
	}

	public GHResponse getResponse() {
		return response;
	}

	public double getDistance() {
		return response.getBest().getDistance();
	}

	public RoutingType getRoutingType() {
		return routingType;
	}

	public Point[] getPoints() {
		return points;
	}

	public GeoPoint getSourceLocation() {
		return sourceLocation;
	}

	public GeoPoint getDestinationLocation() {
		return destinationLocation;
	}
}
