package org.fruct.oss.socialnavigator.routing;

import android.location.Location;

import com.graphhopper.GHResponse;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.utils.EarthSpace;
import org.fruct.oss.socialnavigator.utils.Space;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Point list with deviation handling
 */
public class PathPointList implements Iterable<GeoPoint> {
	public static final double DISTANCE_NEAR = 20;

	private org.fruct.oss.socialnavigator.utils.PointList pointList
			= new org.fruct.oss.socialnavigator.utils.PointList(new EarthSpace(), DISTANCE_NEAR);

	public PathPointList(GHResponse response) {
		PointList ghPointList = response.getPoints();

		for (int i = 0; i < ghPointList.size(); i++) {
			pointList.addPoint(ghPointList.getLatitude(i), ghPointList.getLon(i));
		}
	}

	public PathPointList() {
	}

	public void addPoint(double lat, double lon) {
		pointList.addPoint(lat, lon);
	}

	public boolean isDeviated() {
		return pointList.isDeviated();
	}

	public boolean isCompleted() {
		return pointList.isCompleted();
	}

	public void setLocation(Location location) {
		pointList.setLocation(location.getLatitude(), location.getLongitude());
	}

	@Override
	public Iterator<GeoPoint> iterator() {
		return new Iterator<GeoPoint>() {
			private Iterator<Space.Point> spacePointsIterator = pointList.iterator();

			@Override
			public boolean hasNext() {
				return spacePointsIterator.hasNext();
			}

			@Override
			public GeoPoint next() {
				Space.Point p = spacePointsIterator.next();
				if (p == null)
					return null;
				else
					return new GeoPoint(p.x, p.y);
			}

			@Override
			public void remove() {
				spacePointsIterator.remove();
			}
		};
	}
}
