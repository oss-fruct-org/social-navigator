package org.fruct.oss.socialnavigator;

import android.location.Location;
import android.test.AndroidTestCase;

import org.fruct.oss.socialnavigator.routing.PathPointList;

public class PointListTest extends AndroidTestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private Location createLocation(double lat, double lon) {
		Location loc = new Location("test");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		return loc;
	}

	public void testNormalPath() {
		PathPointList pointList = new PathPointList();
		pointList.addPoint(61.787401, 34.354328);
		pointList.addPoint(61.788529, 34.357847);
		pointList.addPoint(61.789728, 34.356259);
		pointList.addPoint(61.790226, 34.357761);

		pointList.setLocation(createLocation(61.787401, 34.354328));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788529, 34.357847));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.789728, 34.356259));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.790226, 34.357761));
		assertFalse(pointList.isDeviated());
	}
}
