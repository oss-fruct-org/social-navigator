package org.fruct.oss.socialnavigator.gets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Xml;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.fragments.overlays.ObstaclesOverlayFragment;
import org.fruct.oss.socialnavigator.parsers.*;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.Point;
import org.osmdroid.util.GeoPoint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class PointsTask extends GetsTask<List<Point>> {
	private final Category category;
	private final GeoPoint geoPoint;

	private String name;
	private int open;

	public PointsTask(Category category, GeoPoint geoPoint) {
		this.category = category;
		this.geoPoint = geoPoint;
	}

	public String getName() {
		return name;
	}

	public int getOpen() {
		return open;
	}

	@Override
	protected List<Point> parseContent(XmlPullParser parser) throws IOException, XmlPullParserException {
		ArrayList<Point> points = new ArrayList<>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "kml");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "Document");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			switch (tagName) {
			case "name":
				this.name = org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "name");
				break;
			case "open":
				this.open = Integer.parseInt(org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser));
				parser.require(XmlPullParser.END_TAG, null, "open");
				break;
			case "Placemark":
				points.add(Point.parse(parser));
				parser.require(XmlPullParser.END_TAG, null, "Placemark");
				break;
			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "Document");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "kml");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return points;
	}

	@Nullable
	@Override
	protected String getPostQuery() {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			createRequestTop(serializer);
			serializer.startTag(null, "latitude").text(String.valueOf(geoPoint.getLatitude())).endTag(null, "latitude");
			serializer.startTag(null, "longitude").text(String.valueOf(geoPoint.getLongitude())).endTag(null, "longitude");
			serializer.startTag(null, "radius").text(String.valueOf(ObstaclesOverlayFragment.POINT_UPDATE_DISTANCE * 4)).endTag(null, "radius");
			serializer.startTag(null, "category_id").text(String.valueOf(category.getId())).endTag(null, "category_id");
			createRequestBottom(serializer);

			return writer.toString();
		} catch (IOException e) {
			throw new RuntimeException("StringWriter throw IOException", e);
		}
	}

	@NonNull
	@Override
	protected String getRequestUrl() {
		return GETS_SERVER + "/loadPoints.php";
	}
}
