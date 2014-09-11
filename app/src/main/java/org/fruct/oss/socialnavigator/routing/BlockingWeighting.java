package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.util.EdgeIteratorState;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class BlockingWeighting extends FastestWeighting {
	private TIntSet blockedEdgesSet;

	public BlockingWeighting(FlagEncoder encoder, int[] blockedEdges) {
		super(encoder);
		blockedEdgesSet = new TIntHashSet(blockedEdges.length);
		blockedEdgesSet.addAll(blockedEdges);
	}

	@Override
	public double calcWeight(EdgeIteratorState edge, boolean reverse) {
		if (blockedEdgesSet.contains(edge.getEdge())) {
			return Double.POSITIVE_INFINITY;
		} else {
			return super.calcWeight(edge, reverse);
		}
	}

	@Override
	public String toString() {
		return "BLOCKING!" + encoder;
	}
}