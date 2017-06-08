package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.util.Weighting;

import org.fruct.oss.ghpriority.PriorityGraphHopper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomGraphHopper extends PriorityGraphHopper {
	private static final Logger log = LoggerFactory.getLogger(CustomGraphHopper.class);

	private ObstaclesIndex obstaclesIndex;

	public void setObstaclesIndex(ObstaclesIndex obstaclesIndex) {
		this.obstaclesIndex = obstaclesIndex;
	}

	@Override
	public Weighting createWeighting(HintsMap wMap, FlagEncoder encoder) {
		if (wMap.getWeighting().equalsIgnoreCase("half-blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, BlockingWeighting.BlockingType.HALF);
		} else if (wMap.getWeighting().equalsIgnoreCase("blocking")) {
			return new BlockingWeighting(encoder, obstaclesIndex, BlockingWeighting.BlockingType.FULL);
		} else {
			return super.createWeighting(wMap, encoder);
			//return new BlockingWeighting(encoder, obstaclesIndex, BlockingWeighting.BlockingType.NONE);
		}
	}
}