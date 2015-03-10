package org.fruct.oss.socialnavigator.points;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public interface PointsProvider {
	String getProviderName();

	List<Disability> loadDisabilities() throws PointsException;
	List<Category> loadCategories() throws PointsException;
	List<Point> loadPoints(Category category, GeoPoint geoPoint) throws PointsException;


}
