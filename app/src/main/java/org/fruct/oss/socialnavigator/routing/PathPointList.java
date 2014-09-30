package org.fruct.oss.socialnavigator.routing;

import android.location.Location;

import com.graphhopper.GHResponse;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Point list with deviation handling
 */
public class PathPointList implements Iterable<GeoPoint> {
	public static final double DISTANCE_DEVIATE = 20;
	public static final double DISTANCE_NEAR = 20;

	private Deque<GeoPoint> points = new ArrayDeque<GeoPoint>();
	private GeoPoint currentLocation;
	private GeoPoint p1;
	private GeoPoint matchedPoint = new GeoPoint(0, 0);

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

	public boolean isCompleted() {
		return !isDeviated() && points.size() == 1;
	}

	public void setLocation(Location location) {
		currentLocation = new GeoPoint(location);

		if (p1 == null) {
			p1 = points.pollFirst();

			if (points.size() < 2) {
				return;
			}
		}

		GeoPoint p2 = points.peekFirst();

		lastDeviation = Utils.calcDist(location.getLatitude(), location.getLongitude(),
				p1.getLatitude(), p1.getLongitude(),
				p2.getLatitude(), p2.getLongitude(), tmpInt, tmpDouble);

		matchedPoint.setCoordsE6((int) (tmpDouble[0] * 1e6), (int) (tmpDouble[1] * 1e6));

		// Location pass second point of current segment
		if ((tmpInt[0] == 2 || p2.distanceTo(currentLocation) < DISTANCE_NEAR) && points.size() > 1) {
			p1 = null;
			setLocation(location);
		}
	}

	@Override
	public Iterator<GeoPoint> iterator() {
		return new Iterator<GeoPoint>() {
			private boolean first = p1 != null;
			private Iterator<GeoPoint> iter = points.iterator();

			@Override
			public boolean hasNext() {
				return first || iter.hasNext();
			}

			@Override
			public GeoPoint next() {
				GeoPoint ret = first ? matchedPoint : iter.next();
				first = false;
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove");
			}
		};
	}
}
