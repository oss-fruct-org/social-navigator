package org.fruct.oss.socialnavigator;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import junit.framework.AssertionFailedError;

import org.fruct.oss.socialnavigator.routing.PathPointList;
import org.osmdroid.util.GeoPoint;

public class PointListTest extends AndroidTestCase {

	public static final double LAT1 = 61.78737738003173;
	public static final double LON1 = 34.35430537579452;

	public static final double LAT2 = 61.78848092541959;
	public static final double LON2 = 34.35792604332007;

	public static final double LAT3 = 61.78968952490908;
	public static final double LON3 = 34.35622687040238;

	public static final double LAT4 = 61.79015493564046;
	public static final double LON4 = 34.35775136199208;

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

		pointList.addPoint(LAT1, LON1);
		pointList.addPoint(LAT2, LON2);
		pointList.addPoint(LAT3, LON3);
		pointList.addPoint(LAT4, LON4);

		return pointList;
	}

	private void assertPath(PathPointList list, Double... coords) {
		int idx = 0;

		for (GeoPoint point : list) {
			try {
				assertEquals(coords[idx], point.getLatitude(), 0.0001);
				assertEquals(coords[idx + 1], point.getLongitude(), 0.0001);
			} catch (AssertionFailedError ass) {
				throw new AssertionFailedError("Failed on point " + (idx / 2) + ". Was " +
						"(" + point.getLatitude() + " " + point.getLongitude() + "), expected" +
						"(" + coords[idx] + " " + coords[idx + 1] + ")"
				);
			}

			idx += 2;
		}
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

	/*public void testPathIterator() {
		PathPointList pointList = createTestPath1();

		// way1.gpx
		pointList.setLocation(createLocation(61.78737738003173, 34.35403541374217));
		pointList.setLocation(createLocation(61.7876851755867, 34.35460709808831));
		pointList.setLocation(createLocation(61.78791038989373, 34.35584574750495));
		pointList.setLocation(createLocation(61.78806803892682, 34.35686207523142));
		pointList.setLocation(createLocation(61.788408878012234, 34.35768968771927));

		assertFalse(pointList.isDeviated());
		assertFalse(pointList.isCompleted());

		assertPath(pointList, 61.788408878012234, 34.35768968771927,
								 LAT3, LON3, LAT4, LON4);
	}*/
}
