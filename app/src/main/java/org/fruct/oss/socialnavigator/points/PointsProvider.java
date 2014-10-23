package org.fruct.oss.socialnavigator.points;

import java.util.List;

public interface PointsProvider {
	String getProviderName();

	List<Disability> loadDisabilities() throws PointsException;
	List<Category> loadCategories() throws PointsException;
	List<Point> loadPoints(Category category) throws PointsException;
}
