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

	private final String authToken;

	static {
		if (!BuildConfig.DEBUG) {
			GETS_SERVER = "http://gets.cs.petrsu.ru/obstacle/service";
			DISABILITIES_LIST = "http://gets.cs.petrsu.ru/obstacle/config/disabilities.xml";
		} else {
			GETS_SERVER = "http://getsi.ddns.net/getslocal";
			DISABILITIES_LIST = "http://getsi.ddns.net/static/disabilities.xml";
		}
	}


	public GetsProvider(String authToken) {
		this.authToken = authToken;
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
			if (authToken != null)
				serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");
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

			if (authToken != null)
				serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");

			serializer.startTag(null, "latitude").text(String.valueOf(geoPoint.getLatitude())).endTag(null, "latitude");
			serializer.startTag(null, "longitude").text(String.valueOf(geoPoint.getLongitude())).endTag(null, "longitude");
			serializer.startTag(null, "radius").text(String.valueOf(PointsService.POINT_UPDATE_DISTANCE * 4)).endTag(null, "radius");
			serializer.startTag(null, "category_id").text(String.valueOf(category.getId())).endTag(null, "category_id");

			if (authToken != null)
				serializer.startTag(null, "space").text("all").endTag(null, "space");

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
				point.setProvider(Point.GETS_PROVIDER);
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
	public String uploadPoint(Point point) throws PointsException {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			createRequestTop(serializer);

			serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");
			serializer.startTag(null, "category_id").text(String.valueOf(point.getCategory().getId())).endTag(null, "category_id");
			serializer.startTag(null, "title").text(point.getName()).endTag(null, "title");
			serializer.startTag(null, "link").text(point.getUrl()).endTag(null, "link");
			serializer.startTag(null, "latitude").text(String.valueOf(point.getLat())).endTag(null, "latitude");
			serializer.startTag(null, "longitude").text(String.valueOf(point.getLon())).endTag(null, "longitude");

			serializer.startTag(null, "extended_data");
			serializer.startTag(null, "difficulty").text(String.valueOf(point.getDifficulty())).endTag(null, "difficulty");
			serializer.endTag(null, "extended_data");

			createRequestBottom(serializer);

			String request = writer.toString();
			String response = Utils.downloadUrl(GETS_SERVER + "/addPoint.php", request);

			GetsResponse parsedResponse = GetsResponse.parse(response, Kml.class);

			if (parsedResponse.getCode() != 0) {
				throw new PointsException("Can't upload point " + point.getName() + ". Message " + parsedResponse.getMessage());
			}

			Point newPoint = ((Kml) parsedResponse.getContent()).getPoints().get(0);
			return newPoint.getUuid();
		} catch (IOException | GetsException e) {
			throw new PointsException("Can't upload point " + point.getName(), e);
		}
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
