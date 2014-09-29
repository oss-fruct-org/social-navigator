package org.fruct.oss.socialnavigator.utils;

import android.content.res.Resources;
import android.util.TypedValue;

import com.graphhopper.util.DistanceCalcEarth;

import org.fruct.oss.socialnavigator.App;
import org.fruct.oss.socialnavigator.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

public class Utils {

	public static String stringDistance(Resources res, double meters) {
		int kmPart = (int) (meters / 1000);
		int meterPart = (int) (meters - kmPart * 1000);

		if (kmPart != 0 && meterPart != 0) {
			return res.getQuantityString(R.plurals.plural_kilometers, kmPart, kmPart)
				+ " " + res.getQuantityString(R.plurals.plural_meters, meterPart, meterPart);
		} else if (meterPart == 0) {
			return res.getQuantityString(R.plurals.plural_kilometers, kmPart, kmPart);
		} else {
			return res.getQuantityString(R.plurals.plural_meters, meterPart, meterPart);
		}
	}

	public static void copyStream(InputStream input, OutputStream output) throws IOException {
		int bufferSize = 4096;

		byte[] buf = new byte[bufferSize];
		int read;
		while ((read = input.read(buf)) > 0) {
			output.write(buf, 0, read);
		}
	}

	public static int getDP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	public static float getSP(int px) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, px,
				App.getContext().getResources().getDisplayMetrics());
	}

	// Copied and modified from graphhopper's DistanceCalcEarth.java
	private static DistanceCalcEarth distanceCalc = new DistanceCalcEarth();
	public static double calcDist(double r_lat_deg, double r_lon_deg,
						   double a_lat_deg, double a_lon_deg,
						   double b_lat_deg, double b_lon_deg, int[] type, double[] outCoord) {
		type[0] = 0;
		double shrink_factor = cos((toRadians(a_lat_deg) + toRadians(b_lat_deg)) / 2);
		double a_lat = a_lat_deg;
		double a_lon = a_lon_deg * shrink_factor;

		double b_lat = b_lat_deg;
		double b_lon = b_lon_deg * shrink_factor;

		double r_lat = r_lat_deg;
		double r_lon = r_lon_deg * shrink_factor;

		double delta_lon = b_lon - a_lon;
		double delta_lat = b_lat - a_lat;

		if (delta_lat == 0) {
			// special case: horizontal edge
			outCoord[0] = a_lat_deg;
			outCoord[1] = r_lon_deg;
			return distanceCalc.calcDist(a_lat_deg, r_lon_deg, r_lat_deg, r_lon_deg);
		}
		if (delta_lon == 0) {
			// special case: vertical edge
			outCoord[0] = r_lat_deg;
			outCoord[1] = a_lon_deg;
			return distanceCalc.calcDist(r_lat_deg, a_lon_deg, r_lat_deg, r_lon_deg);
		}

		double norm = delta_lon * delta_lon + delta_lat * delta_lat;
		double factor = ((r_lon - a_lon) * delta_lon + (r_lat - a_lat) * delta_lat) / norm;

		if (factor > 1) {
			type[0] = 2;
			factor = 1;
		} else if (factor < 0) {
			type[0] = 1;
			factor = 0;
		}

		// x,y is projection of r onto segment a-b
		double c_lon = a_lon + factor * delta_lon;
		double c_lat = a_lat + factor * delta_lat;

		outCoord[0] = c_lat;
		outCoord[1] = c_lon / shrink_factor;

		return distanceCalc.calcDist(c_lat, c_lon / shrink_factor, r_lat_deg, r_lon_deg);
	}
}
