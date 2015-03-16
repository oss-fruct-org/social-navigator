package org.fruct.oss.socialnavigator.points;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class ArrayPointsProvider implements PointsProvider {
	private List<Category> categories = new ArrayList<Category>();
	private List<Point> pointList = new ArrayList<Point>();
	private String providerName;

	public ArrayPointsProvider(String providerName) {
		this.providerName = providerName;
	}

	public void setCategories(String... names) {
		categories = new ArrayList<Category>();
		int c = 0;
		for (String name : names) {
			categories.add(new Category(name, name, "http://example.com", "", ++c, false));
		}
	}

	public void addPointDesc(String name, String description, String url, String categoryName, double lat, double lon, int diff) {
		Category cat = null;
		for (Category exCat : categories) {
			if (exCat.getDescription().equals(categoryName)) {
				cat = exCat;
				break;
			}
		}

		if (cat == null) {
			throw new IllegalArgumentException("Category " + categoryName + " don't exist");
		}

		pointList.add(new Point(name, description, url, lat, lon, cat, Point.TEST_PROVIDER, name, diff));
	}

	@Override
	public String getProviderName() {
		return providerName;
	}

	@Override
	public List<Disability> loadDisabilities() throws PointsException {
		ArrayList<Disability> disabilities = new ArrayList<Disability>();
		disabilities.add(new Disability("test disablility", new int[] {1}));
		return disabilities;
	}

	@Override
	public List<Category> loadCategories() {
		return categories;
	}

	@Override
	public List<Point> loadPoints(Category category, GeoPoint geoPoint) throws PointsException {
		List<Point> catPoints = new ArrayList<Point>();
		for (Point point : pointList) {
			if (point.getCategory().equals(category)) {
				catPoints.add(point);
			}
		}
/*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}

		if (Math.random() > 0.8)
			throw new PointsException("Test");
*/
		return catPoints;
	}

	@Override
	public String uploadPoint(Point point) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
