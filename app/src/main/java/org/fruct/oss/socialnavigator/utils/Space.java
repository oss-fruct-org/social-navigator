package org.fruct.oss.socialnavigator.utils;

public interface Space {
	Point newPoint(double x, double y);
	double dist(Point a, Point b);
	double bearing(Point a, Point b);
	double projectedDist(Point r, Point a, Point b, int[] tmpInt, Point out);

	public class Point {
		public double x, y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
}
