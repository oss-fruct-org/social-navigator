package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc2D;
import com.graphhopper.util.shapes.GHPoint3D;

import org.fruct.oss.socialnavigator.points.Point;
import org.osmdroid.util.GeoPoint;

import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class CustomGraphHopper extends GraphHopper {
	private int[] blockedEdgesIds;
	private int[] blockedEdgesDifficulties;

	public void updateBlockedEdges(List<Point> points) {
		GeoPoint tmpGeoPoint = new GeoPoint(0, 0);

		TIntList blockedEdgesIdsList = new TIntArrayList();
		TIntList blockedEdgesDifficultiesList = new TIntArrayList();

		DistanceCalc2D distanceCalc = new DistanceCalc2D();
		LocationIndex index = getLocationIndex();

		for (Point point : points) {
			GeoPoint geoPoint = point.toGeoPoint();
			QueryResult result = index.findClosest(geoPoint.getLatitude(), geoPoint.getLongitude(), EdgeFilter.ALL_EDGES);

			if (result.isValid()) {
				GHPoint3D snappedPoint = result.getSnappedPoint();
				tmpGeoPoint.setCoordsE6((int) (snappedPoint.getLat() * 1e6), (int) (snappedPoint.getLon() * 1e6));

				if (tmpGeoPoint.distanceTo(geoPoint) < 10) {
					blockedEdgesIdsList.add(result.getClosestEdge().getEdge());
					blockedEdgesDifficultiesList.add(point.getDifficulty());
				}
			}
		}

		blockedEdgesDifficulties = blockedEdgesDifficultiesList.toArray();
		blockedEdgesIds = blockedEdgesIdsList.toArray();
	}

	@Override
	public Weighting createWeighting(String weighting, FlagEncoder encoder) {
		if (weighting.equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, blockedEdgesIds, blockedEdgesDifficulties, true);
		} else if (weighting.equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, blockedEdgesIds, blockedEdgesDifficulties, false);
		} else {
			return super.createWeighting(weighting, encoder);
		}
	}
}