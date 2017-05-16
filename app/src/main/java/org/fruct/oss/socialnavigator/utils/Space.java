package org.fruct.oss.socialnavigator.utils;

import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.util.GeoPoint;

public interface Space {
	Point newPoint(double x, double y);
	double dist(Point a, Point b);
	double bearing(Point a, Point b);
	double projectedDist(Point r, Point a, Point b, int[] tmpInt, Point out);

	public class Point implements Parcelable {
		public double x, y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public static Point fromGeoPoint(GeoPoint pt) {
			return new Point(pt.getLatitude() / 100000, pt.getLongitude() / 100000);
		}

		public String toString() {
			return this.x + ", " + this.y;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeDouble(x);
			dest.writeDouble(y);
		}

		public static final Creator<Point> CREATOR = new Creator<Point>() {
			@Override
			public Point createFromParcel(Parcel source) {
				return new Point(source.readDouble(), source.readDouble());
			}

			@Override
			public Point[] newArray(int size) {
				return new Point[size];
			}
		};
	}
}
