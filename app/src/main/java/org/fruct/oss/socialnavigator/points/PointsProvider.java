package org.fruct.oss.socialnavigator.points;

import java.util.List;

public interface PointsProvider {
	String getProviderName();
	List<Category> loadCategories();
	List<Point> loadPoints(Category category);
}
