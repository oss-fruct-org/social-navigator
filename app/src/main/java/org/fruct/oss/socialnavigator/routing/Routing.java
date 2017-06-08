package org.fruct.oss.socialnavigator.routing;

import android.content.Context;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootFlagEncoder;
import com.graphhopper.util.PointList;

import org.fruct.oss.ghpriority.FootPriorityFlagEncoder;
import org.fruct.oss.socialnavigator.points.Point;
import org.jetbrains.annotations.Nullable;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Routing {
	private static final Logger log = LoggerFactory.getLogger(Routing.class);

	private CustomGraphHopper gh;
	private ObstaclesIndex obstaclesIndex;
	private boolean isReady;

	public synchronized void loadFromPref(Context context, String path) {
		log.info("Loading graphhopper from path {}", path);
		if (gh != null) {
			gh.close();
			isReady = false;
		}

		gh = (CustomGraphHopper) new CustomGraphHopper().forMobile();
		gh.setEncodingManager(new EncodingManager(new ArrayList<FlagEncoder>(4) {{
			add(new FootFlagEncoder());
			add(new FootPriorityFlagEncoder());
		}}, 8));

		gh.setCHEnabled(false);

		if (path != null) {
			if (!gh.load(path)) {
				gh.close();
				gh = null;
				log.error("Can't initialize graphhopper in " + path);
				throw new RuntimeException("Can't initialize graphhopper in " + path);
			}
			log.info("Graphhopper successfully loaded");
			isReady = true;
		} else {
			log.error("Can't initialize graphhopper in null path");
		}
	}

	public synchronized boolean isReady() {
		return isReady;
	}

	public synchronized void close() {
		if (gh != null) {
			isReady = false;
			gh.close();
			gh = null;
		}

		if (obstaclesIndex != null) {
			obstaclesIndex.clear();
		}
	}

	@Nullable
	public synchronized ChoicePath route(GeoPoint from, GeoPoint to, RoutingType routingType) {
		if (gh == null) {
			log.warn("Routing with null graphhopper");
			return null;
		}

		gh.setObstaclesIndex(obstaclesIndex);

		GHRequest request = new GHRequest(from.getLatitude(), from.getLongitude(),
				to.getLatitude(), to.getLongitude());
		request.setVehicle(routingType.getVehicle());
		request.setWeighting(routingType.getWeighting());

		try {
			GHResponse response = gh.route(request);

			if (response.hasErrors()) {
				log.warn("Graphhopper routing errors:");
				for (Throwable throwable : response.getErrors()) {
					log.warn("Error: ", throwable);
				}
				return null;
			}

			PointList pointList = response.getBest().getPoints();

			if (pointList.size() < 2) {
				log.warn("Path found but is empty");
				return null;
			}
			log.debug("Found " + pointList.size() + " points");

			for (int i = 0; i < pointList.getSize() - 1; i++) {
				obstaclesIndex.clearchosenObstacles(pointList.getLat(i), pointList.getLon(i),
						pointList.getLat(i + 1), pointList.getLon(i + 1),
						BlockingWeighting.BLOCK_RADIUS);
			}
			Set<Point> pointsOnPath = new HashSet<Point>();
			log.info("Searching obstacles on path");
			//for(double BlockRadius = 1.0 ; BlockRadius <= BlockingWeighting.BLOCK_RADIUS; BlockRadius = BlockRadius+1.0) {
				for (int i = 0; i < pointList.getSize() - 1; i++) {
					pointsOnPath.addAll(obstaclesIndex.queryByEdge(
							pointList.getLat(i), pointList.getLon(i),
							pointList.getLat(i + 1), pointList.getLon(i + 1),
                            BlockingWeighting.BLOCK_RADIUS));
				}
			//}

			log.info("{} obstacles on path found", pointsOnPath.size());

			return new ChoicePath(response, routingType,
					pointsOnPath.toArray(new Point[pointsOnPath.size()]),
					from, to);
		} catch (Exception ex) {
			log.error("Routing error for routing type: {}", routingType, ex);
			return null;
		}
	}

	public synchronized void setObstacles(List<Point> points) {
		long startTime = System.currentTimeMillis();

		obstaclesIndex = new ObstaclesIndex(gh.getGraphHopperStorage());
		for (Point point : points) {
			obstaclesIndex.insertPoint(point);
		}
		obstaclesIndex.initialize();

		log.debug("Blocked edges calculation took {} ms", System.currentTimeMillis() - startTime);
	}
}
