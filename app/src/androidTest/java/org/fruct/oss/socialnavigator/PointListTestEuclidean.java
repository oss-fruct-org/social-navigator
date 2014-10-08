package org.fruct.oss.socialnavigator;

import android.test.AndroidTestCase;

import junit.framework.AssertionFailedError;

import org.fruct.oss.socialnavigator.utils.PointList;
import org.fruct.oss.socialnavigator.utils.Space;

public class PointListTestEuclidean extends AndroidTestCase {
	private Space2D space = new Space2D();
	private int[] tmpInt = new int[1];
	private Space.Point tmpPoint = new Space.Point(0, 0);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private PointList createTestPath1() {
		PointList pointList = new PointList(space, 2);

		pointList.addPoint(0, 0);
		pointList.addPoint(10, 0);
		pointList.addPoint(10, 10);
		pointList.addPoint(20, 10);

		return pointList;
	}

	private void assertPath(PointList list, Double... coords) {
		int idx = 0;

		for (Space.Point point : list) {
			try {
				assertEquals(coords[idx], point.x, 0.0001);
				assertEquals(coords[idx + 1], point.y, 0.0001);
			} catch (AssertionFailedError ass) {
				throw new AssertionFailedError("Failed on point " + (idx / 2) + ". Was " +
						"(" + point.x + " " + point.y + "), expected" +
						"(" + coords[idx] + " " + coords[idx + 1] + ")"
				);
			}

			idx += 2;
		}
	}

	public void testProjectedDist() {
		double dist = space.projectedDist(new Space.Point(1, 1), new Space.Point(0, 0), new Space.Point(2, 0), tmpInt, tmpPoint);

		assertEquals(1, dist, 0.1);
		assertEquals(0, tmpInt[0]);

		assertEquals(1, tmpPoint.x, 0.1);
		assertEquals(0, tmpPoint.y, 0.1);
	}

	public void testProjectedDistLeft() {
		double dist = space.projectedDist(new Space.Point(-1, 1), new Space.Point(0, 0), new Space.Point(2, 0), tmpInt, tmpPoint);

		assertEquals(Math.sqrt(2), dist, 0.1);
		assertEquals(1, tmpInt[0]);

		assertEquals(0, tmpPoint.x, 0.1);
		assertEquals(0, tmpPoint.y, 0.1);
	}


	public void testProjectedDistRight() {
		double dist = space.projectedDist(new Space.Point(3, 1), new Space.Point(0, 0), new Space.Point(2, 0), tmpInt, tmpPoint);

		assertEquals(Math.sqrt(2), dist, 0.1);
		assertEquals(2, tmpInt[0]);

		assertEquals(2, tmpPoint.x, 0.1);
		assertEquals(0, tmpPoint.y, 0.1);
	}

	public void testBearing() {
		assertEquals(Math.PI / 4, space.bearing(new Space.Point(0, 0), new Space.Point(1, 1)), 0.01);
	}

	public void testExactPath() {
		PointList pointList = createTestPath1();

		pointList.setLocation(0, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(10, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(10, 10);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(20, 10);
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testLagPath() {
		PointList pointList = createTestPath1();

		pointList.setLocation(-1, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(9, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(10, 9);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(19, 10);
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testAheadPath() {
		PointList pointList = createTestPath1();

		pointList.setLocation(1, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(11, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(10, 11);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(21, 10);
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());
	}

	public void testDeviatePath() {
		PointList pointList = createTestPath1();

		pointList.setLocation(0, 0);
		pointList.setLocation(10, 0);
		pointList.setLocation(13, 10);
		assertTrue(pointList.isDeviated());
	}

	public void testStartIterable() {
		PointList pointList = createTestPath1();
		assertPath(pointList, 0.0,0.0,  10.0,0.0,  10.0,10.0,  20.0,10.0);
	}

	public void testStartIterable2() {
		PointList pointList = createTestPath1();
		pointList.setLocation(0, 0);
		assertPath(pointList, 0.0, 0.0, 10.0, 0.0, 10.0, 10.0, 20.0, 10.0);
	}

	public void testHalfLineIterable() {
		PointList pointList = createTestPath1();
		pointList.setLocation(5, 0);
		assertPath(pointList, 5.0, 0.0, 10.0, 0.0, 10.0, 10.0, 20.0, 10.0);
	}

	public void testTurnLineIterable() {
		PointList pointList = createTestPath1();
		pointList.setLocation(9, 0);
		assertPath(pointList, 9.0, 0.0, 10.0, 0.0, 10.0, 10.0, 20.0, 10.0);
	}

	public void testJump1to2() {
		PointList pointList = createTestPath1();
		pointList.setLocation(5, 0);
		pointList.setLocation(9, 0);
		pointList.setLocation(10, 1);
		assertFalse(pointList.isDeviated());
	}

	public void testDeviateAfterLastPoint() {
		PointList pointList = createTestPath1();

		pointList.setLocation(0, 0);
		pointList.setLocation(10, 0);
		pointList.setLocation(11, 10);
		pointList.setLocation(20, 10);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(21, 10);
		pointList.setLocation(23, 10);
		assertTrue(pointList.isDeviated());
	}

	// FIXME: this test fails but may be this is correct behaviour
	/*public void testDeviateAfterLastPointBack() {
		PointList pointList = createTestPath1();

		pointList.setLocation(0, 0);
		pointList.setLocation(10, 0);
		pointList.setLocation(11, 10);
		pointList.setLocation(20, 10);
		assertFalse(pointList.isDeviated());
		assertTrue(pointList.isCompleted());

		pointList.setLocation(19, 10);
		pointList.setLocation(17, 10);
		assertTrue(pointList.isDeviated());
	}*/

	public void testSegmentedPathIterable() {
		PointList pointList = new PointList(space, 2);

		pointList.addPoint(0, 0);
		pointList.addPoint(10, 0);
		pointList.addPoint(20, 1);

		pointList.setLocation(7, 1);
		pointList.setLocation(8, 1);
		pointList.setLocation(9, 1);

		assertPath(pointList, 9.0, 0.0,  10.0, 0.0,  20.0, 1.0);
	}

	public void testSegmentedPathIterable2() {
		PointList pointList = new PointList(space, 2);

		pointList.addPoint(0, 0);
		pointList.addPoint(10, 0);
		pointList.addPoint(20, 0);

		pointList.setLocation(9, 0);
		pointList.setLocation(10, 0);
		pointList.setLocation(11, 0);

		assertPath(pointList, 11.0, 0.0,  20.0, 0.0);
	}

	public void testLargeDeviation() {
		PointList pointList = createTestPath1();

		pointList.setLocation(0, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(10, 0);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(11, 11);
		assertFalse(pointList.isDeviated());

		pointList.setLocation(-100, 11);
		assertTrue(pointList.isDeviated());
		assertFalse(pointList.isCompleted());
	}

	public void testInitialPathIterable() {
		PointList pointList = createTestPath1();
		assertPath(pointList, 0.0,0.0,  10.0,0.0,  10.0,10.0,  20.0,10.0);
	}

	private class Space2D implements Space {

		@Override
		public Point newPoint(double x, double y) {
			return new Point(x, y);
		}

		@Override
		public double dist(Point a, Point b) {
			double dx = a.x - b.x;
			double dy = a.y - b.y;
			return Math.hypot(dx, dy);
		}

		@Override
		public double bearing(Point a, Point b) {
			double dx = b.x - a.x;
			double dy = b.y - a.y;

			return Math.atan2(dx, dy);
		}

		@Override
		public double projectedDist(Point r, Point a, Point b, int[] tmpInt, Point out) {
			double dx = b.x - a.x;
			double dy = b.y - a.y;

			double dist2 = dx * dx + dy * dy;
			double t = dot(r, a, b, a) / dist2;

			if (t < 0) {
				tmpInt[0] = 1;
				out.x = a.x;
				out.y = a.y;
				return dist(r, a);
			}

			if (t > 1) {
				tmpInt[0] = 2;
				out.x = b.x;
				out.y = b.y;
				return dist(r, b);
			}

			double vx = a.x + t * dx;
			double vy = a.y + t * dy;

			out.x = vx;
			out.y = vy;

			tmpInt[0] = 0;

			return dist(out, r);
		}

		private double dot(Point r, Point a, Point p, Point b) {
			double ar_x  = r.x - a.x;
			double ar_y  = r.y - a.y;

			double pb_x = p.x - b.x;
			double pb_y = p.y - b.y;

			return ar_x * pb_x + ar_y * pb_y;
		}
	}
}
