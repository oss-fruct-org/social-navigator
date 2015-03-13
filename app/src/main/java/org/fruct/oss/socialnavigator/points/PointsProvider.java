package org.fruct.oss.socialnavigator.points;

import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.List;

public interface PointsProvider {
	String getProviderName();

	List<Disability> loadDisabilities() throws PointsException;
	List<Category> loadCategories() throws PointsException;
	List<Point> loadPoints(Category category, GeoPoint geoPoint) throws PointsException;

	/**
	 * @param local point
	 * @return uuid of new point
	 */
	String uploadPoint(Point point) throws PointsException;
}
