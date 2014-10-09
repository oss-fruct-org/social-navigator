package org.fruct.oss.socialnavigator.utils;

import android.location.Location;

import org.osmdroid.util.GeoPoint;

public class EarthSpace implements Space {
	private float[] ret = new float[1];
	private double[] tmpDouble = new double[2];

	private GeoPoint p1 = new GeoPoint(0, 0);
	private GeoPoint p2 = new GeoPoint(0, 0);

	@Override
	public Point newPoint(double x, double y) {
		return new Point(x, y);
	}

	@Override
	public double dist(Point a, Point b) {
		Location.distanceBetween(a.x, a.y, b.x, b.y, ret);
		return ret[0];
	}

	@Override
	public double bearing(Point a, Point b) {
		p1.setCoordsE6((int) (a.x * 1e6), (int) (a.y * 1e6));
		p2.setCoordsE6((int) (b.x * 1e6), (int) (b.y * 1e6));
		return Math.toRadians(p1.bearingTo(p2));
	}

	@Override
	public double projectedDist(Point r, Point a, Point b, int[] tmpInt, Point out) {
		double ret = Utils.calcDist(r.x, r.y, a.x, a.y, b.x, b.y, tmpInt, tmpDouble);
		out.x = tmpDouble[0];
		out.y = tmpDouble[1];
		return ret;
	}
}
