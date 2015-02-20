package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.storage.Graph;

import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.utils.Function;
import org.fruct.oss.socialnavigator.utils.Utils;

/**
 * Test obstacle index that doesn't use quadtree
 */
public class LinearObstaclesIndex extends ObstaclesIndex {
	private int[] outInt = new int[1];
	private double[] outDouble = new double[2];

	public LinearObstaclesIndex(Graph graph) {
		super(graph);
	}

	@Override
	public void initialize() {

	}

	@Override
	public void clear() {

	}

	@Override
	public void queryByEdge(double aLat, double aLon, double bLat, double bLon, double radius, Function<Point> func) {
		for (Point point : points) {
			double dist = Utils.calcDist(point.getLat(), point.getLon(), aLat, aLon, bLat, bLon,
					outInt, outDouble);
			if (dist < radius) {
				func.call(point);
			}
		}
	}
}
