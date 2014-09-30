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

	private PathPointList createTestPath1() {
		PathPointList pointList = new PathPointList();
		pointList.addPoint(61.787401, 34.354328);
		pointList.addPoint(61.788529, 34.357847);
		pointList.addPoint(61.789728, 34.356259);
		pointList.addPoint(61.790226, 34.357761);
		return pointList;
	}

	public void testNormalPath() {
		PathPointList pointList = createTestPath1();

		pointList.setLocation(createLocation(61.787401, 34.354328));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788529, 34.357847));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.789728, 34.356259));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.790226, 34.357761));
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testNormalPathSmallDeviations() {
		PathPointList pointList = createTestPath1();

		pointList.setLocation(createLocation(61.787371, 34.354457));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788470, 34.357801));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.789606, 34.356152));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.790226, 34.357826));
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testNormalPathSmallDeviationsAfter() {
		PathPointList pointList = createTestPath1();

		pointList.setLocation(createLocation(61.787354, 34.354231));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788486, 34.358044));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.789724,34.356134));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.790232,34.357926));
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testPathDeviation() {
		PathPointList pointList = createTestPath1();

		pointList.setLocation(createLocation(61.787354, 34.354231));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788486, 34.358044));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.789724,34.356134));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.719724,34.356134));
		assertTrue(pointList.isDeviated());
		assertFalse(pointList.isCompleted());
	}

	public void testOnLine() {
		PathPointList pointList = createTestPath1();

		pointList.setLocation(createLocation(61.787389,34.354460));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788497,34.357872));
		assertFalse(pointList.isDeviated());

		pointList.setLocation(createLocation(61.788766,34.358698));
		assertTrue(pointList.isDeviated());
	}
}
