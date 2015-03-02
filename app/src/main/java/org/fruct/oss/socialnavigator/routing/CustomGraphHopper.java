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

import org.fruct.oss.ghpriority.PriorityGraphHopper;
import org.fruct.oss.socialnavigator.points.Point;
import org.jetbrains.annotations.Nullable;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class CustomGraphHopper extends PriorityGraphHopper {
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
		log.info("Searching path for {} {}", request.getVehicle(), request.getWeighting());

		List<Path> paths = getPaths(request, response);
		if (response.hasErrors() && paths.size() == 0) {
			log.warn("No path found");
			return null;
		}

		Path ghPath = paths.get(0);
		PointList pointList = ghPath.calcPoints();

		Set<Point> pointsOnPath = new HashSet<Point>();
		if (pointList.size() < 2) {
			log.warn("Path found but is empty");
			return null;
		}

		log.info("Searching obstacles on path");
		for (int i = 0; i < pointList.getSize() - 1; i++) {
			pointsOnPath.addAll(obstaclesIndex.queryByEdge(
					pointList.getLat(i), pointList.getLon(i),
					pointList.getLat(i + 1), pointList.getLon(i + 1),
					BlockingWeighting.BLOCK_RADIUS));
		}

		log.info("{} obstacles on path found", pointsOnPath.size());

		return new RoutingService.Path(pointList,
				ghPath.getDistance(),
				routingType,
				pointsOnPath.toArray(new Point[pointsOnPath.size()]));

	}

	@Override
	public Weighting createWeighting(WeightingMap wMap, FlagEncoder encoder) {
		if (wMap.getWeighting().equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, true);
		} else if (wMap.getWeighting().equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, false);
		} else {
			return super.createWeighting(wMap, encoder);
		}
	}
}