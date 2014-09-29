package org.fruct.oss.socialnavigator.routing;

import android.location.Location;

import com.graphhopper.GHResponse;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Point list with deviation handling
 */
public class PathPointList {
	public static final double DISTANCE_DEVIATE = 20;
	public static final double DISTANCE_NEAR = 20;

	private Deque<GeoPoint> points = new ArrayDeque<GeoPoint>();
	private GeoPoint currentLocation;
	private GeoPoint p1;
	private double lastDeviation;

	private double[] tmpDouble = new double[2];
	private int[] tmpInt = new int[2];

	public PathPointList(GHResponse response) {
		PointList pointList = response.getPoints();
		InstructionList instructions = response.getInstructions();

		for (int i = 0; i < pointList.size(); i++) {
			points.add(new GeoPoint(pointList.getLatitude(i), pointList.getLon(i)));
		}
	}

	public PathPointList() {
	}

	public void addPoint(double lat, double lon) {
		points.add(new GeoPoint(lat, lon));
	}

	public boolean isDeviated() {
		return lastDeviation >= DISTANCE_DEVIATE;
	}

	public void setLocation(Location location) {
		currentLocation = new GeoPoint(location);

		if (points.size() < 2) {
			return;
		}

		if (p1 == null) {
			p1 = points.pollFirst();
		}

		GeoPoint p2 = points.peekFirst();

		lastDeviation = Utils.calcDist(location.getLatitude(), location.getLongitude(),
				p1.getLatitude(), p1.getLongitude(),
				p2.getLatitude(), p2.getLongitude(), tmpInt, tmpDouble);

		// Location pass second point of current segment
		if (tmpInt[0] == 2) {
			p1 = null;
			setLocation(location);
		}
	}
}
