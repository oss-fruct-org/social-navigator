package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.FootPriorityWeighting;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.EdgeIteratorState;

import org.jetbrains.annotations.NotNull;

import gnu.trove.impl.hash.TIntIntHash;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class BlockingWeighting extends FootPriorityWeighting {
	public static final double BLOCK_RADIUS = 20;

	private final boolean half;
	private final ObstaclesIndex obstaclesIndex;

	public BlockingWeighting(@NotNull FlagEncoder encoder,
							 @NotNull ObstaclesIndex obstaclesIndex,
							 boolean half) {
		super(encoder);
		this.half = half;
		this.obstaclesIndex = obstaclesIndex;
	}

	@Override
	public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
		if (obstaclesIndex.checkEdgeBlocked(edge, BLOCK_RADIUS, half)) {
			return Double.POSITIVE_INFINITY;
		} else {
			return super.calcWeight(edge, reverse, prevOrNextEdgeId);
		}
	}

	@Override
	public String toString() {
		return "BLOCKING" + (half ? "-HALF!" : "!") + encoder;
	}
}