package org.fruct.oss.socialnavigator.points;

import java.util.ArrayList;
import java.util.Collections;
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

		pointList.add(new Point(name, description, url, lat, lon, cat.getId(), Point.TEST_PROVIDER, name));
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

		try {
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}

		if (Math.random() > 0.8)
			throw new RuntimeException("Test");

		return catPoints;
	}
}
