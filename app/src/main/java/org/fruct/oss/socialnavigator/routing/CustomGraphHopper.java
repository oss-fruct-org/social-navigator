package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.WeightingMap;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class CustomGraphHopper extends GraphHopper {
	private static final Logger log = LoggerFactory.getLogger(CustomGraphHopper.class);

	private ObstaclesIndex obstaclesIndex;

	public void updateBlockedEdges(List<Point> points) {
		long startTime = System.currentTimeMillis();

		obstaclesIndex = new ObstaclesIndex(getGraph());
		for (Point point : points) {
			obstaclesIndex.insertPoint(point);
		}
		obstaclesIndex.initialize();

		log.debug("Blocked edges calculation took {} ms", System.currentTimeMillis() - startTime);
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
			pointsOnPath.addAll(obstaclesIndex.queryByEdge(edge, BlockingWeighting.BLOCK_RADIUS));
		}

		return new RoutingService.Path(pointList,
				ghPath.getDistance(),
				routingType,
				pointsOnPath.toArray(new Point[pointsOnPath.size()]));

	}

	@Override
	public Weighting createWeighting(WeightingMap wMap, FlagEncoder encoder) {
		String weighting = wMap.getWeighting();
		if (weighting.equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, true);
		} else if (weighting.equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, false);
		} else {
			return super.createWeighting(wMap, encoder);
		}
	}
}