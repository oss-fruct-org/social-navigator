package org.fruct.oss.socialnavigator.routing;

import android.util.Log;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;

import org.fruct.oss.ghpriority.FootPriorityWeighting;
import org.fruct.oss.socialnavigator.points.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlockingWeighting extends FootPriorityWeighting {

    public static enum BlockingType {FULL, HALF, NONE};

	public static final double BLOCK_RADIUS = 10;

    public static final double DIFFICULTY_WEIGHT = 0.1;

	private final BlockingType type;
	private final ObstaclesIndex obstaclesIndex;
	private final String encoder;

	public BlockingWeighting(@NotNull FlagEncoder encoder,
							 @NotNull ObstaclesIndex obstaclesIndex,
							 BlockingType type) {
		super(encoder);

		this.encoder = encoder.toString();

		this.type = type;
		this.obstaclesIndex = obstaclesIndex;
	}

	@Override
	public double calcWeight(EdgeIteratorState edge, boolean reverse, int prevOrNextEdgeId) {
		Log.d(getClass().getSimpleName(), "Call calcWeight() for " + edge.toString() + " distance=" + edge.getDistance());
		if (type != BlockingType.NONE && obstaclesIndex.checkEdgeBlocked(edge, BLOCK_RADIUS, type == BlockingType.HALF)) {
			return Double.POSITIVE_INFINITY;
		} else {
            double weight = super.calcWeight(edge, reverse, prevOrNextEdgeId);

            // ищем препятствия на данном участке маршрута
            PointList nodes = edge.fetchWayGeometry(3);
            final List<Point> ret = new ArrayList<>();

            ret.addAll(obstaclesIndex.queryByEdge(
                    nodes.getLat(0), nodes.getLon(0),
                    nodes.getLat(1), nodes.getLon(1),
                    BlockingWeighting.BLOCK_RADIUS));

            if (ret.size() > 0) {
                Log.d(getClass().getSimpleName(), "For edge " + edge.toString() + " found " + ret.size() + " obstacles");


                // добавляем 2^сложность препятствий * веc в минутах
                for (Point pt : ret) {
                    weight += Math.pow(2.0, pt.getDifficulty()) * DIFFICULTY_WEIGHT;
                }

                Log.d(getClass().getSimpleName(), "For edge " + edge.toString() + " final weight = " + weight);
            }

            return weight;
		}
	}

	@Override
	public String toString() {
		return "BLOCKING" + (type == BlockingType.HALF ? "-HALF!" : "!") + encoder;
	}
}