package org.fruct.oss.socialnavigator.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class PointList implements Iterable<Space.Point> {
	private final Space space;
	private final double nearDistance;

	private final Deque<Space.Point> points = new ArrayDeque<Space.Point>();
	private double lastDeviation;

	private Space.Point currentLocation;

	private Space.Point segmentStartPoint;
	private boolean onTurnPoint;


	private final int[] tmpInt = new int[1];
	private final Space.Point tmpPoint = new Space.Point(0, 0);
	private Space.Point matchedPoint = new Space.Point(0, 0);

	public PointList(Space space, double nearDistance) {
		this.space = space;
		this.nearDistance = nearDistance;
	}

	public void addPoint(double x, double y) {
		points.add(new Space.Point(x, y));
	}

	public boolean isDeviated() {
		return lastDeviation >= nearDistance;
	}

	public boolean isCompleted() {
		return !isDeviated() && points.size() == 1
				&& space.dist(matchedPoint, points.getFirst()) < nearDistance;
	}

	public void setLocation(double x, double y) {
		currentLocation = new Space.Point(x, y);

		if (segmentStartPoint == null) {
			segmentStartPoint = points.pollFirst();

			if (points.size() < 1) {
				return;
			}
		}

		Space.Point segmentEndPoint = points.peekFirst();

		if (onTurnPoint) {
			lastDeviation = space.dist(currentLocation, segmentEndPoint);
			if (lastDeviation > nearDistance) {
				segmentStartPoint = null;
				onTurnPoint = false;
				setLocation(x, y);
				return;
			}
		}

		lastDeviation = space.projectedDist(currentLocation, segmentStartPoint, segmentEndPoint, tmpInt, tmpPoint);

		matchedPoint.x = tmpPoint.x;
		matchedPoint.y = tmpPoint.y;

		// Location near second point of current segment
		if (space.dist(segmentEndPoint, matchedPoint) < nearDistance || tmpInt[0] == 2) {
			onTurnPoint = true;
		}
	}

	@Override
	public Iterator<Space.Point> iterator() {
		return new Iterator<Space.Point>() {
			private boolean first = segmentStartPoint != null;

			private Iterator<Space.Point> iter = points.iterator();

			@Override
			public boolean hasNext() {
				return first ||  iter.hasNext();
			}

			@Override
			public Space.Point next() {
				if (first) {
					first = false;
					return matchedPoint;
				}

				return iter.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove");
			}
		};

	}
}
