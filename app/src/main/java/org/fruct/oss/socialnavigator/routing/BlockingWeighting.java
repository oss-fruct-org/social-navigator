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
	private final boolean half;
	private final TIntIntMap blockedEdgesMap;

	@Deprecated
	public BlockingWeighting(FlagEncoder encoder, int[] edgeIds, int[] difficulties, boolean half) {
		super(encoder);
		this.half = half;

		blockedEdgesMap = new TIntIntHashMap(edgeIds.length, 0.5f, -1, -1);
		for (int i = 0; i < edgeIds.length; i++) {
			blockedEdgesMap.put(edgeIds[i], difficulties[i]);
		}
	}

	public BlockingWeighting(@NotNull FlagEncoder encoder,
							 @NotNull TIntObjectMap<BlockedEdge> blockedEdges,
							 boolean half) {
		super(encoder);
		this.half = half;

		blockedEdgesMap = new TIntIntHashMap(blockedEdges.size(), 0.5f, -1, -1);
		for (BlockedEdge blockedEdge : blockedEdges.valueCollection()) {
			blockedEdgesMap.put(blockedEdge.edge, blockedEdge.difficulty);
		}
	}

	@Override
	public double calcWeight(EdgeIteratorState edge, boolean reverse) {
		int difficulty = blockedEdgesMap.get(edge.getEdge());

		if (difficulty == -1) {
			return super.calcWeight(edge, reverse);
		}

		if (difficulty < 5 && half) {
			return super.calcWeight(edge, reverse);
		} else {
			return Double.POSITIVE_INFINITY;
		}
	}

	@Override
	public String toString() {
		return "BLOCKING" + (half ? "-HALF!" : "!") + encoder;
	}
}