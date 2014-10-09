package org.fruct.oss.socialnavigator.utils;

import android.content.res.Resources;
import android.util.TypedValue;

import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.App;
import org.fruct.oss.socialnavigator.R;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.toRadians;

public class Utils {
	private static final Logger log = LoggerFactory.getLogger(Utils.class);

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

	/*public static List<Turn> findTurns(List<GeoPoint> points) {
		// Two point line can't has turns
		if (points.size() < 3)
			return Collections.emptyList();

		double lastBearing = points.get(0).bearingTo(points.get(1));

		ArrayList<Turn> turns = new ArrayList<Turn>();
		for (int i = 1; i < points.size() - 1; i++) {
			double bearing = points.get(i).bearingTo(points.get(i + 1));
			double relBearing = Utils.normalizeAngle(bearing - lastBearing);

			double diff = Math.abs(relBearing);
			int turnDirection = relBearing > 0 ? -1 : 1;

			int turnSharpness;
			if (diff < 11) {
				continue;
			} else if (diff < 40) {
				turnSharpness = 1;
			} else if (diff < 103) {
				turnSharpness = 2;
			} else {
				turnSharpness = 3;
			}

			lastBearing = bearing;

			turns.add(new Turn(points.get(i), turnSharpness, turnDirection));
		}
		return turns;
	}*/

	public static double normalizeAngle(double degree) {
		return (StrictMath.IEEEremainder(degree, 360));
	}

	public static double normalizeAngleRad(double radian) {
		return (StrictMath.IEEEremainder(radian, 2 * Math.PI));
	}


	public static List<GeoPoint> toList(PointList pointList) {
		ArrayList<GeoPoint> ret = new ArrayList<GeoPoint>(pointList.size());

		for (int i = 0; i < pointList.size(); i++) {
			ret.add(new GeoPoint(pointList.getLatitude(i), pointList.getLongitude(i)));
		}

		return ret;
	}

	public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		int depth = 1;

		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException("Parser must be on start tag");
		}

		while (depth > 0) {
			switch (parser.next()) {
			case XmlPullParser.START_TAG:
				depth++;
				break;
			case XmlPullParser.END_TAG:
				depth--;
				break;
			}
		}
	}

	public static String inputStreamToString(InputStream stream) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
		return readerToString(reader);
	}

	public static String readerToString(Reader reader) throws IOException {
		StringBuilder builder = new StringBuilder();
		int bufferSize = 4096;
		char[] buf = new char[bufferSize];

		int readed;
		while ((readed = reader.read(buf)) > 0) {
			builder.append(buf, 0, readed);
		}

		return builder.toString();
	}

	public static String downloadUrl(String urlString, String postQuery) throws IOException {
		HttpURLConnection conn = null;
		InputStream responseStream = null;

		try {
			URL url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);
			conn.setRequestMethod(postQuery == null ? "GET" : "POST");
			conn.setDoInput(true);
			conn.setDoOutput(postQuery != null);
			conn.setRequestProperty("User-Agent", "RoadSigns/0.2 (http://oss.fruct.org/projects/roadsigns/)");
			conn.setRequestProperty("Content-Type", "Content-Type: text/xml;charset=utf-8");

			if (postQuery != null) {
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
				writer.write(postQuery);
				writer.flush();
				writer.close();
			}

			log.trace("Request url {} data {}", urlString, postQuery);
			conn.connect();

			int responseCode = conn.getResponseCode();
			responseStream = conn.getInputStream();
			String response = Utils.inputStreamToString(responseStream);

			log.trace("Response code {}, response {}", responseCode, response);

			return response;
		} finally {
			if (conn != null)
				conn.disconnect();

			if (responseStream != null)
				responseStream.close();
		}
	}
}
