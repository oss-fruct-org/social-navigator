package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.points.Point;

/**
* Created by ivashov on 03.03.15.
*/
public class ChoicePath {
	private final GHResponse response;
	private final Point[] points;
	private final RoutingType routingType;

	public ChoicePath(GHResponse response, RoutingType routingType, Point[] points) {
		this.response = response;
		this.routingType = routingType;
		this.points = points;
	}

	public GHResponse getResponse() {
		return response;
	}

	public double getDistance() {
		return response.getDistance();
	}

	public RoutingType getRoutingType() {
		return routingType;
	}

	public Point[] getPoints() {
		return points;
	}
}
