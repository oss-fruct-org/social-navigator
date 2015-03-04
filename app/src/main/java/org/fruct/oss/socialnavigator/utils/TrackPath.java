package org.fruct.oss.socialnavigator.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TrackPath<T> {
	private final Space space;

	private final List<Space.Point> trackPoints = new ArrayList<Space.Point>();

	private Segment firstSegment;

	private Segment lastSegment;

	private final TreeMap<ProjectedPoint, T> points = new TreeMap<ProjectedPoint, T>();

	private double currentLength = 0;

	private transient int[] tmpInt = new int[2];
	private transient Space.Point tmpPoint = new Space.Point(0, 0);

	public TrackPath(Space space, List<Space.Point> points) {
		this.space = space;

		for (Space.Point point : points) {
			addTrackPoint(point.x, point.y);
		}
	}

	public TrackPath(Space space, Space.Point... points) {
		this(space, Arrays.asList(points));
	}

	public void addPoint(double x, double y, T pointData) {
		ProjectedPoint projectedPoint = projectPoint(x, y);
		points.put(projectedPoint, pointData);
	}

	public Result<T> query(double x, double y) {
		Result<T> result = new Result<T>();

		ProjectedPoint projectedPoint = projectPoint(x, y);

		Map.Entry<ProjectedPoint, T> entryFloor = points.floorEntry(projectedPoint);
		Map.Entry<ProjectedPoint, T> entryCeiling = points.ceilingEntry(projectedPoint);

		Map.Entry<ProjectedPoint, T> nearestEntry = null;
		if (entryFloor != null && entryCeiling != null) {
			double dist1 = space.dist(entryFloor.getKey(), projectedPoint);
			double dist2 = space.dist(entryCeiling.getKey(), projectedPoint);
			if (dist1 < dist2) {
				nearestEntry = entryFloor;
			} else {
				nearestEntry = entryCeiling;
			}
		} else if (entryFloor != null) {
			nearestEntry = entryFloor;
		} else if (entryCeiling != null) {
			nearestEntry = entryCeiling;
		}

		if (nearestEntry != null) {
			result.nextPointDistance = space.dist(nearestEntry.getKey(), projectedPoint);
			result.nextPointData = nearestEntry.getValue();
		}

		// Find remaining path
		result.remainingPath.add(new Space.Point(projectedPoint.projX, projectedPoint.projY));

		Segment currentSegment = projectedPoint.nearestSegment.next;
		while (currentSegment != null) {
			result.remainingPath.add(currentSegment.a);
			currentSegment = currentSegment.next;
		}

		return result;
	}

	private void addTrackPoint(double x, double y) {
		Space.Point newTrackPoint = new Space.Point(x, y);
		trackPoints.add(newTrackPoint);

		if (lastSegment == null) {
			lastSegment = firstSegment = new Segment(newTrackPoint, null, 0.0, 0.0);
		} else {
			lastSegment.segmentLength = space.dist(lastSegment.a, newTrackPoint);
			Segment newSegment = new Segment(newTrackPoint, null,
					lastSegment.accumulatedDistance + lastSegment.segmentLength, 0);
			lastSegment.next = newSegment;

			lastSegment = newSegment;
		}
	}

	private ProjectedPoint projectPoint(double x, double y) {
		Space.Point sourcePoint = new Space.Point(x, y);

		Segment nearestSegment = null;
		double nearestDist = Double.MAX_VALUE;
		double nearestX = 0, nearestY = 0;

		Segment currentSegment = firstSegment;
		while (currentSegment.next != null) {
			double dist = space.projectedDist(sourcePoint, currentSegment.a, currentSegment.next.a,
					tmpInt, tmpPoint);

			if (dist < nearestDist) {
				nearestDist = dist;
				nearestSegment = currentSegment;
				nearestX = tmpPoint.x;
				nearestY = tmpPoint.y;
			}

			currentSegment = currentSegment.next;
		}

		tmpPoint.x = nearestX;
		tmpPoint.y = nearestY;

		return new ProjectedPoint(x, y, tmpPoint.x, tmpPoint.y, nearestSegment,
				nearestSegment.accumulatedDistance + space.dist(nearestSegment.a, tmpPoint));
	}

	private static class ProjectedPoint extends Space.Point implements Comparable<ProjectedPoint> {
		private double projX;
		private double projY;
		private Segment nearestSegment;
		private double distanceOnPath;

		public ProjectedPoint(double x, double y, double projX, double projY,
							  Segment nearestSegment, double distanceOnPath) {
			super(x, y);
			this.projX = projX;
			this.projY = projY;
			this.nearestSegment = nearestSegment;
			this.distanceOnPath = distanceOnPath;
		}


		@Override
		public int compareTo(@NonNull ProjectedPoint another) {
			return Double.compare(distanceOnPath, another.distanceOnPath);
		}
	}

	private static class Segment {
		private final Space.Point a;
		private final double accumulatedDistance;
		private Segment next;
		private double segmentLength;

		private Segment(Space.Point a, Segment next, double accumulatedDistance, double segmentLength) {
			this.a = a;
			this.next = next;
			this.accumulatedDistance = accumulatedDistance;
			this.segmentLength = segmentLength;
		}
	}

	public static class Result<T> {
		public T nextPointData;
		public double nextPointDistance;

		public List<Space.Point> remainingPath = new ArrayList<Space.Point>();
	}
}
