package org.fruct.oss.socialnavigator.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class PointList implements Iterable<Space.Point> {
	private final Space space;
	private final double nearDistance;
	private final double deviateDistance;

	private final Deque<Space.Point> points = new ArrayDeque<Space.Point>();
	private double lastDeviation;

	private Space.Point currentLocation;

	private Space.Point nextTarget;


	private final int[] tmpInt = new int[1];
	private final Space.Point tmpPoint = new Space.Point(0, 0);
	private Space.Point matchedPoint = new Space.Point(0, 0);

	public PointList(Space space, double nearDistance, double deviateDistance) {
		this.space = space;
		this.nearDistance = nearDistance;
		this.deviateDistance = deviateDistance;
	}

	public void addPoint(double x, double y) {
		points.add(new Space.Point(x, y));
	}

	public boolean isDeviated() {
		return lastDeviation >= deviateDistance;
	}

	public boolean isCompleted() {
		return !isDeviated() && points.size() == 1
				&& space.dist(currentLocation, points.getFirst()) < nearDistance;
	}

	public void setLocation(double x, double y) {
		currentLocation = new Space.Point(x, y);

		if (nextTarget == null) {
			nextTarget = points.pollFirst();

			if (points.size() < 2) {
				return;
			}
		}

		Space.Point p2 = points.peekFirst();

		lastDeviation = space.projectedDist(currentLocation, nextTarget, p2, tmpInt, tmpPoint);

		matchedPoint.x = tmpPoint.x;
		matchedPoint.y = tmpPoint.y;

		// Location pass second point of current segment
		if ((tmpInt[0] == 2 || space.dist(p2, currentLocation) < nearDistance) && points.size() > 1) {
			nextTarget = null;
			setLocation(x, y);
		}
	}

	@Override
	public Iterator<Space.Point> iterator() {
		return new Iterator<Space.Point>() {
			private boolean first = nextTarget != null;
			private Iterator<Space.Point> iter = points.iterator();

			@Override
			public boolean hasNext() {
				return first || iter.hasNext();
			}

			@Override
			public Space.Point next() {
				Space.Point ret = first ? matchedPoint : iter.next();
				first = false;
				return ret;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove");
			}
		};

	}
}
