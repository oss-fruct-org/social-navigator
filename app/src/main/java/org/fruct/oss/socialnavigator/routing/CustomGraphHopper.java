package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.routing.util.HintsMap;
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

	public void setObstaclesIndex(ObstaclesIndex obstaclesIndex) {
		this.obstaclesIndex = obstaclesIndex;
	}

	@Override
	public Weighting createWeighting(HintsMap wMap, FlagEncoder encoder) {
		if (wMap.getWeighting().equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, true);
		} else if (wMap.getWeighting().equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, false);
		} else {
			return super.createWeighting(wMap, encoder);
		}
	}
}