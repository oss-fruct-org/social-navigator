package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DouglasPeucker;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PathMerger;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;

import org.fruct.oss.socialnavigator.points.Point;
import org.jetbrains.annotations.Nullable;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class CustomGraphHopper extends GraphHopper {
	private TIntObjectMap<BlockedEdge> blockedEdges = new TIntObjectHashMap<BlockedEdge>();

	public void updateBlockedEdges(List<Point> points) {
		GeoPoint tmpGeoPoint = new GeoPoint(0, 0);

		blockedEdges.clear();

		LocationIndex index = getLocationIndex();

		for (Point point : points) {
			GeoPoint geoPoint = point.toGeoPoint();
			QueryResult result = index.findClosest(geoPoint.getLatitude(), geoPoint.getLongitude(), EdgeFilter.ALL_EDGES);

			if (result.isValid()) {
				GHPoint3D snappedPoint = result.getSnappedPoint();
				tmpGeoPoint.setCoordsE6((int) (snappedPoint.getLat() * 1e6), (int) (snappedPoint.getLon() * 1e6));

				if (tmpGeoPoint.distanceTo(geoPoint) < 10) {
					int edge = result.getClosestEdge().getEdge();
					blockedEdges.put(edge, new BlockedEdge(point.getDifficulty(), edge, point));
				}
			}
		}
	}

	@Nullable
	public RoutingService.Path routePath(GHRequest request, RoutingType routingType) {
		GHResponse response = new GHResponse();
		List<Path> paths = getPaths(request, response);
		if (response.hasErrors() && paths.size() == 0)
			return null;

		Path ghPath = paths.get(0);

		List<Point> pointsOnPath = new ArrayList<Point>();

		PointList pointList = ghPath.calcPoints();
		List<EdgeIteratorState> edges = ghPath.calcEdges();
		for (EdgeIteratorState edge : edges) {
			int edgeId = edge.getEdge();
			BlockedEdge blockedEdge = blockedEdges.get(edgeId);
			if (blockedEdge != null) {
				pointsOnPath.add(blockedEdge.blockingPoint);
			}
		}

		return new RoutingService.Path(pointList,
				ghPath.getDistance(),
				routingType,
				pointsOnPath.toArray(new Point[pointsOnPath.size()]));

	}

	@Override
	public Weighting createWeighting(String weighting, FlagEncoder encoder) {
		if (weighting.equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, blockedEdges, true);
		} else if (weighting.equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, blockedEdges, false);
		} else {
			return super.createWeighting(weighting, encoder);
		}
	}
}