package org.fruct.oss.socialnavigator.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;

public class PointList implements Iterable<Space.Point> {
	private final Space space;
	private final double nearDistance;

	private final ArrayList<Space.Point> points = new ArrayList<Space.Point>();
	private final ArrayList<Segment> segments = new ArrayList<Segment>();
	private int segmentIdx = 0;

	private double lastDeviation;

	private Space.Point currentLocation;

	private final int[] tmpInt = new int[1];
	private final Space.Point tmpPoint = new Space.Point(0, 0);

	private final int[] tmpInt2 = new int[1];
	private final Space.Point tmpPoint2 = new Space.Point(0, 0);

	private Space.Point matchedPoint = new Space.Point(0, 0);

	private boolean isInitialized;
	private boolean isHasFix;

	public PointList(Space space, double nearDistance) {
		this.space = space;
		this.nearDistance = nearDistance;
	}

	public void addPoint(double x, double y) {
		if (isInitialized)
			throw new IllegalStateException("Can't add point to started path");

		points.add(new Space.Point(x, y));
	}

	public void initialize() {
		if (isInitialized) {
			return;
		}

		for (int i = 1; i < points.size(); i++) {
			segments.add(new Segment(points.get(i - 1), points.get(i)));
		}

		isInitialized = true;
	}

	public boolean isDeviated() {
		return lastDeviation >= nearDistance;
	}

	public boolean isCompleted() {
		return segmentIdx == segments.size() - 1 && space.dist(currentLocation, segments.get(segmentIdx).p2) < nearDistance;
	}

	public Turn checkTurn() {
		initialize();

		for (int i = segmentIdx; i < segments.size() - 1; i++) {
			Segment currentSegment = segments.get(i);
			Segment nextSegment = segments.get(i + 1);
			Turn turn = checkTurn(currentSegment.p1, nextSegment.p1, nextSegment.p2);
			if (turn != null)
				return turn;
		}

		return null;
	}

	public void setLocation(double x, double y) {
		initialize();

		currentLocation = new Space.Point(x, y);
		isHasFix = true;

		Segment currentSegment = segments.get(segmentIdx);
		Segment nextSegment = segmentIdx + 1 < segments.size() ? segments.get(segmentIdx + 1) : null;

		double dist1 = space.projectedDist(currentLocation, currentSegment.p1, currentSegment.p2, tmpInt, tmpPoint);
		double dist2 = 0;
		if (nextSegment != null) {
			dist2 = space.projectedDist(currentLocation, nextSegment.p1, nextSegment.p2, tmpInt2, tmpPoint2);
		}

		if (nextSegment != null && (dist2 < dist1 || tmpInt[0] == 2)) {
			segmentIdx++;
			matchedPoint.x = tmpPoint2.x;
			matchedPoint.y = tmpPoint2.y;
			lastDeviation = dist2;
		} else {
			matchedPoint.x = tmpPoint.x;
			matchedPoint.y = tmpPoint.y;
			lastDeviation = dist1;
		}
	}


	private Turn checkTurn(Space.Point a, Space.Point b, Space.Point c) {
		double bearing1 = space.bearing(a, b);
		double bearing2 = space.bearing(b, c);

		double relBearing = Utils.normalizeAngleRad(bearing2 - bearing1);
		double diff = Math.abs(relBearing);
		int turnDirection = relBearing > 0 ? -1 : 1;

		int turnSharpness;
		if (diff < 0.2) {
			return null;
		} else if (diff < 0.8) {
			turnSharpness = 1;
		} else if (diff < 1.8) {
			turnSharpness = 2;
		} else {
			turnSharpness = 3;
		}

		return new Turn(b, turnSharpness, turnDirection);
	}

	private class Segment {
		private Segment(Space.Point p1, Space.Point p2) {
			this.p1 = p1;
			this.p2 = p2;
		}

		Space.Point p1;
		Space.Point p2;
	}

	@Override
	public Iterator<Space.Point> iterator() {
		initialize();

		return new Iterator<Space.Point>() {
			private Space.Point firstPoint = !isHasFix ? segments.get(0).p1 : matchedPoint;

			private int idx = segmentIdx;

			{
				//firstPoint = null;
			}

			@Override
			public boolean hasNext() {
				return firstPoint != null || idx < segments.size();
			}

			@Override
			public Space.Point next() {
				if (firstPoint != null) {
					Space.Point ret = firstPoint;
					firstPoint = null;
					return ret;
				}

				return segments.get(idx++).p2;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can't remove");
			}
		};

	}
}
