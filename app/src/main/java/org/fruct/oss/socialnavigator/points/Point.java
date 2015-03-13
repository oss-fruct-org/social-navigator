package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.util.GeoPoint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.StringTokenizer;

public class Point implements Parcelable {
	public static final String LOCAL_PROVIDER = "local_provider";
	public static final String TEST_PROVIDER = "test_provider";
	public static final String GETS_PROVIDER = "gets_provider";

	private String name;
	private String description;
	private String url;
	private int latE6;
	private int lonE6;
	private Category category;
	private String provider;
	private String uuid;
	private int difficulty;

	private boolean isPrivate;

	public Point(String name, String description, String url, double lat, double lon, Category category, String provider, String uuid, int difficulty) {
		this(name, description, url, (int) (lat * 1e6), (int) (lon * 1e6), category, provider, uuid, difficulty);
	}

	public Point(String name, String description, String url, int latE6, int lonE6, Category category, String provider, String uuid, int difficulty) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.latE6 = latE6;
		this.lonE6 = lonE6;
		this.category = category;
		this.provider = provider;
		this.uuid = uuid;
		this.difficulty = difficulty;
	}

	public Point(Cursor cursor, int offset) {
		this.name = cursor.getString(offset);
		this.description = cursor.getString(offset + 1);
		this.url = cursor.getString(offset + 2);
		this.latE6 = cursor.getInt(offset + 3);
		this.lonE6 = cursor.getInt(offset + 4);
		this.provider = cursor.getString(offset + 5);
		this.uuid = cursor.getString(offset + 6);
		this.difficulty = cursor.getInt(offset + 7);
		this.isPrivate = cursor.getInt(offset + 8) != 0;

		this.category = new Category(cursor, offset + 9);
	}

	public Point(Parcel source) {
		this.name = source.readString();
		this.description = source.readString();
		this.url = source.readString();
		this.latE6 = source.readInt();
		this.lonE6 = source.readInt();
		this.category = source.readParcelable(Point.class.getClassLoader());
		this.provider = source.readString();
		this.uuid = source.readString();
		this.difficulty = source.readInt();
		this.isPrivate = source.readInt() != 0;
	}

	private Point() {

	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public int getLatE6() {
		return latE6;
	}

	public int getLonE6() {
		return lonE6;
	}

	public double getLat() {
		return latE6 / 1e6;
	}

	public double getLon() {
		return lonE6 / 1e6;
	}


	public Category getCategory() {
		return category;
	}

	public String getProvider() {
		return provider;
	}

	public String getUuid() {
		return uuid;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public boolean isPrivate() {
		return isPrivate;
	}


	public void setCategory(Category category) {
		this.category = category;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public void setPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}


	// TODO: can be optimized
	public GeoPoint toGeoPoint() {
		return new GeoPoint(latE6, lonE6);
	}

	public void setCoordinates(String coordinates) {
		StringTokenizer tok = new StringTokenizer(coordinates, ",", false);

		double longitude = Double.parseDouble(tok.nextToken());
		double latitude = Double.parseDouble(tok.nextToken());
		latE6 = (int) (latitude * 1e6);
		lonE6 = (int) (longitude * 1e6);
	}

	public static Point parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "Placemark");
		Point point = new Point();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			switch (tagName) {
			case "name":
				point.name = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "name");
				break;

			case "description":
				point.description = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "description");
				break;

			case "Point":
				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "coordinates");

				point.setCoordinates(GetsResponse.readText(parser));

				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "Point");
				break;

			case "ExtendedData":
				readExtendedData(parser, point);
				parser.require(XmlPullParser.END_TAG, null, "ExtendedData");
				break;

			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		if (point.uuid == null) {
			point.uuid = "kml-point-" + point.name + "-" + point.getLatE6() + "-" + point.getLonE6();
		}

		return point;
	}

	private static void readExtendedData(XmlPullParser parser, Point point) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "ExtendedData");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("Data")) {
				String key = parser.getAttributeValue(null, "name");
				if (key == null)
					throw new XmlPullParserException("Data tag have to have attribute 'name'");


				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "value");
				String value = GetsResponse.readText(parser);

				if (key.equals("uuid"))
					point.uuid = value;
				else if (key.equals("difficulty") || key.equals("rating")) {
					try {
						point.difficulty = Integer.parseInt(value);
					} catch (NumberFormatException ignored) {
					}
				}
				/*else if (key.equals("time"))
					point.time = value;
				else if (key.equals("photo"))
					point.photoUrl = value;
				else if (key.equals("audio"))
					point.audioUrl = value;
				else if (key.equals("access"))
					point.setPrivate(value.equals("rw"));
				else if (key.equals("idx"))
					point.setIdx(Long.parseLong(value));*/

				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "Data");
			} else {
				XmlUtil.skip(parser);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (!uuid.equals(point.uuid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(url);
		dest.writeInt(latE6);
		dest.writeInt(lonE6);
		dest.writeParcelable(category, flags);
		dest.writeString(provider);
		dest.writeString(uuid);
		dest.writeInt(difficulty);
		dest.writeInt(isPrivate ? 1 : 0);
	}

	public static final Creator<Point> CREATOR = new Creator<Point>() {
		@Override
		public Point createFromParcel(Parcel source) {
			return new Point(source);
		}

		@Override
		public Point[] newArray(int size) {
			return new Point[size];
		}
	};
}
