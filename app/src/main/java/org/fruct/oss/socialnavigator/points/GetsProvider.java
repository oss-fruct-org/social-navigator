package org.fruct.oss.socialnavigator.points;

import android.util.Xml;

import org.fruct.oss.socialnavigator.BuildConfig;
import org.fruct.oss.socialnavigator.fragments.overlays.ObstaclesOverlayFragment;
import org.fruct.oss.socialnavigator.parsers.CategoriesContent;
import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.parsers.Kml;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.util.GeoPoint;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

public class GetsProvider implements PointsProvider {
	public static final String GETS_SERVER;
	public static final String DISABILITIES_LIST;

	static {
		if (!BuildConfig.DEBUG) {
			GETS_SERVER = "http://gets.cs.petrsu.ru/obstacle/service";
			DISABILITIES_LIST = "http://gets.cs.petrsu.ru/obstacle/config/disabilities.xml";
		} else {
			GETS_SERVER = "http://getsi.ddns.net/getslocal";
			DISABILITIES_LIST = "http://getsi.ddns.net/static/disabilities.xml";
		}
	}

	@Override
	public String getProviderName() {
		return "gets-provider";
	}

	@Override
	public List<Disability> loadDisabilities() throws PointsException {
		try {
			String response = Utils.downloadUrl(DISABILITIES_LIST, null);
			return Disability.parse(new StringReader(response));
		} catch (Exception ex) {
			throw new PointsException("Network error during disabilities request", ex);
		}
	}

	@Override
	public List<Category> loadCategories() throws PointsException {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			createRequestTop(serializer);
			createRequestBottom(serializer);
			String request = writer.toString();
			String response = Utils.downloadUrl(GETS_SERVER + "/getCategories.php", request);

			GetsResponse parsedResponse = GetsResponse.parse(response, CategoriesContent.class);

			if (parsedResponse.getCode() != 0) {
				throw new PointsException("Gets server return error code "
						+ parsedResponse.getCode() + ": " + parsedResponse.getMessage());
			}

			return ((CategoriesContent) parsedResponse.getContent()).getCategories();
		} catch (IOException ex) {
			throw new PointsException("Network error during categories request", ex);
		} catch (GetsException ex) {
			throw new PointsException("Gets server return incorrect answer during categories request", ex);
		}
	}

	@Override
	public List<Point> loadPoints(Category category, GeoPoint geoPoint) throws PointsException {
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
			String request = writer.toString();
			String response = Utils.downloadUrl(GETS_SERVER + "/loadPoints.php", request);

			GetsResponse parsedResponse = GetsResponse.parse(response, Kml.class);

			if (parsedResponse.getCode() != 0) {
				throw new PointsException("Gets server return error code "
						+ parsedResponse.getCode() + ": " + parsedResponse.getMessage());
			}

			List<Point> points = ((Kml) parsedResponse.getContent()).getPoints();
			for (Point point : points) {
				point.setCategory(category);
			}
			return points;
		} catch (IOException ex) {
			throw new PointsException("Network error during points request", ex);
		} catch (GetsException ex) {
			throw new PointsException("Gets server return incorrect answer during points request", ex);
		}
	}

	@Override
	public void uploadPoint(Point point) {

	}

	private void createRequestTop(XmlSerializer xmlSerializer) throws IOException {
		xmlSerializer.startDocument("UTF-8", true);
		xmlSerializer.startTag(null, "request").startTag(null, "params");
	}

	private void createRequestBottom(XmlSerializer xmlSerializer) throws IOException {
		xmlSerializer.endTag(null, "params").endTag(null, "request");
		xmlSerializer.endDocument();
	}
}
