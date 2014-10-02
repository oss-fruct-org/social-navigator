package org.fruct.oss.socialnavigator.points;

import java.util.List;

public interface PointsProvider {
	String getProviderName();
	List<Category> loadCategories() throws PointsException;
	List<Point> loadPoints(Category category) throws PointsException;
}
