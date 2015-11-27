package org.fruct.oss.socialnavigator.routing;

import android.util.SparseBooleanArray;

import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;

import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.utils.Function;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.fruct.oss.socialnavigator.utils.quadtree.Func;
import org.fruct.oss.socialnavigator.utils.quadtree.Node;
import org.fruct.oss.socialnavigator.utils.quadtree.QuadTree;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class ObstaclesIndex {
	private final NodeAccess nodeAccess;
	private QuadTree quadtree;

	protected List<Point> points = new ArrayList<Point>();

	private double latMin = -90;
	private double lonMin = -180;
	private double latMax = 90;
	private double lonMax = 180;

	private boolean isInitialized = false;

	private int[] outInt = new int[1];
	private double[] outDouble = new double[2];

	private TIntSet fullBlockCache = new TIntHashSet();
	private TIntSet fullBlockProcessed = new TIntHashSet();

	private TIntSet halfBlockCache = new TIntHashSet();
	private TIntSet halfBlockProcessed = new TIntHashSet();

	public ObstaclesIndex(Graph graph) {
		this.nodeAccess = graph.getNodeAccess();
	}

	public void insertPoint(Point point) {
		if (isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex already initialized");

		double lat = point.getLat();
		double lon = point.getLon();

		if (latMin > lat) latMin = lat;
		if (latMax < lat) latMax = lat;
		if (lonMin > lon) lonMin = lon;
		if (lonMax < lon) lonMax = lon;

		points.add(point);
	}

	public void initialize() {
		if (isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex already initialized");

		quadtree = new QuadTree(latMin, lonMin, latMax, lonMax);

		for (Point point : points) {
			quadtree.set(point.getLat(), point.getLon(), point);
		}

		isInitialized = true;
	}

	public void clear() {
		isInitialized = false;
		quadtree.clear();
	}

	public void queryByEdge(final double aLat, final double aLon, final double bLat, final double bLon,
			final double radius, final Function<Point> func) {
		if (!isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex not initialized");

		final double rectALat;
		final double rectALon;
		final double rectBLat;
		final double rectBLon;

		if (aLat < bLat) {
			rectALat = aLat;
			rectBLat = bLat;
		} else {
			rectALat = bLat;
			rectBLat = aLat;
		}

		if (aLon < bLon) {
			rectALon = aLon;
			rectBLon = bLon;
		} else {
			rectALon = bLon;
			rectBLon = aLon;
		}

		double latCenter = (rectALat + rectBLat) / 2;
		double degreesRadius = Math.toDegrees((radius / GeoPoint.RADIUS_EARTH_METERS)
				* Math.cos(Math.toRadians(latCenter)));

		quadtree.navigate(quadtree.getRootNode(), new Func() {
			@Override
			public void call(QuadTree quadTree, Node node) {
				org.fruct.oss.socialnavigator.utils.quadtree.Point quadtreePoint = node.getPoint();
				double nodeLat = quadtreePoint.getX();
				double nodeLon = quadtreePoint.getY();

				double dist = Utils.calcDist(nodeLat, nodeLon,
						aLat, aLon, bLat, bLon, outInt, outDouble);

				if (dist < radius && !node.getUsed()) {
					node.setUsed(true);
					//node.setMaster(aLat,aLon,bLat,bLon);
					func.call((Point) node.getPoint().getValue());
				}
			}
		}, rectALat - degreesRadius, rectALon + degreesRadius, rectBLat - degreesRadius, rectBLon + degreesRadius);
	}

	public void queryByEdge(EdgeIteratorState edge, final double radius, final Function<Point> func) {
		final double aLat = nodeAccess.getLat(edge.getBaseNode());
		final double aLon = nodeAccess.getLon(edge.getBaseNode());
		final double bLat = nodeAccess.getLat(edge.getAdjNode());
		final double bLon = nodeAccess.getLon(edge.getAdjNode());
		//queryByEdge(aLat, aLon, bLat, bLon, radius, func);
		if (!isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex not initialized");

		final double rectALat;
		final double rectALon;
		final double rectBLat;
		final double rectBLon;

		if (aLat < bLat) {
			rectALat = aLat;
			rectBLat = bLat;
		} else {
			rectALat = bLat;
			rectBLat = aLat;
		}

		if (aLon < bLon) {
			rectALon = aLon;
			rectBLon = bLon;
		} else {
			rectALon = bLon;
			rectBLon = aLon;
		}

		double latCenter = (rectALat + rectBLat) / 2;
		double degreesRadius = Math.toDegrees((radius / GeoPoint.RADIUS_EARTH_METERS)
				* Math.cos(Math.toRadians(latCenter)));

		quadtree.navigate(quadtree.getRootNode(), new Func() {
			@Override
			public void call(QuadTree quadTree, Node node) {
				org.fruct.oss.socialnavigator.utils.quadtree.Point quadtreePoint = node.getPoint();
				double nodeLat = quadtreePoint.getX();
				double nodeLon = quadtreePoint.getY();

				double dist = Utils.calcDist(nodeLat, nodeLon,
						aLat, aLon, bLat, bLon, outInt, outDouble);

				if (dist < radius && !node.getUsed2()) {
					node.setUsed2(true);
					//node.setMaster(aLat,aLon,bLat,bLon);
					func.call((Point) node.getPoint().getValue());
				}
			}
		}, rectALat - degreesRadius, rectALon + degreesRadius, rectBLat - degreesRadius, rectBLon + degreesRadius);
	}

	public void clearchosenObstacles(final double aLat, final double aLon, final double bLat, final double bLon, final double radius)
	{
		if (!isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex not initialized");

		final double rectALat;
		final double rectALon;
		final double rectBLat;
		final double rectBLon;

		if (aLat < bLat) {
			rectALat = aLat;
			rectBLat = bLat;
		} else {
			rectALat = bLat;
			rectBLat = aLat;
		}

		if (aLon < bLon) {
			rectALon = aLon;
			rectBLon = bLon;
		} else {
			rectALon = bLon;
			rectBLon = aLon;
		}
		double latCenter = (rectALat + rectBLat) / 2;
		double degreesRadius = Math.toDegrees((radius / GeoPoint.RADIUS_EARTH_METERS)
				* Math.cos(Math.toRadians(latCenter)));
		quadtree.navigate(quadtree.getRootNode(), new Func() {
			@Override
			public void call(QuadTree quadTree, Node node) {
				org.fruct.oss.socialnavigator.utils.quadtree.Point quadtreePoint = node.getPoint();
				node.setUsed(false);
				node.setUsed2(false);
			}
		}, rectALat - degreesRadius, rectALon + degreesRadius, rectBLat - degreesRadius, rectBLon + degreesRadius);
		return;
	}

	public List<Point> queryByEdge(final double aLat, final double aLon, final double bLat, final double bLon,
								   final double radius) {
		final ArrayList<Point> ret = new ArrayList<Point>();

		queryByEdge(aLat, aLon, bLat, bLon, radius, new Function<Point>() {
			@Override

			public void call(Point point) {
				ret.add(point);
			}
		});

		return ret;
	}

	public boolean checkEdgeBlocked(EdgeIteratorState edge, final double radius, final boolean half) {
		final TIntSet cacheProcessed = half ? halfBlockProcessed : fullBlockProcessed;
		final TIntSet cacheBlocked = half ? halfBlockCache : fullBlockCache;

		int edgeId = edge.getEdge();
		if (cacheProcessed.contains(edgeId)) {
			return cacheBlocked.contains(edgeId);
		}

		final boolean[] result = new boolean[1];
		//for(double BlockRadius = 1.0 ; BlockRadius <= radius; BlockRadius = BlockRadius+1.0) {
			queryByEdge(edge, radius, new Function<Point>() {
				@Override
				public void call(Point point) {
					if (!half || point.getDifficulty() >= 5) {
						result[0] = true;
					}
				}
			});
		//}

		cacheProcessed.add(edgeId);

		if (result[0])
			cacheBlocked.add(edgeId);

		return result[0];
	}
}
