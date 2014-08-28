package org.fruct.oss.socialnavigator.points;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArrayPointsProvider implements PointsProvider {
	private List<Category> categories = Collections.emptyList();
	private List<Point> pointList = Collections.emptyList();
	private String providerName;

	public ArrayPointsProvider(String providerName) {
		this.providerName = providerName;
	}

	public void setCategories(String... names) {
		categories = new ArrayList<Category>();
		int c = 0;
		for (String name : names) {
			categories.add(new Category(name, name, "http://example.com", ++c));
		}
	}

	public void addPointDesc(String name, String description, String url, String categoryName, double lat, double lon) {
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

		pointList.add(new Point(name, description, url, lat, lon, cat.getId(), Point.TEST_PROVIDER));
	}

	@Override
	public String getProviderName() {
		return providerName;
	}

	@Override
	public List<Category> loadCategories() {
		return categories;
	}

	@Override
	public List<Point> loadPoints(Category category) {
		List<Point> catPoints = new ArrayList<Point>();
		for (Point point : pointList) {
			if (point.getCategoryId() == category.getId()) {
				catPoints.add(point);
			}
		}

		return catPoints;
	}
}
