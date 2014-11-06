package org.fruct.oss.socialnavigator.routing;

import org.fruct.oss.socialnavigator.points.Point;

class BlockedEdge {
	final Point blockingPoint;
	final int edge;
	final int difficulty;

	BlockedEdge(int difficulty, int edge, Point blockingPoint) {
		this.difficulty = difficulty;
		this.edge = edge;
		this.blockingPoint = blockingPoint;
	}
}
