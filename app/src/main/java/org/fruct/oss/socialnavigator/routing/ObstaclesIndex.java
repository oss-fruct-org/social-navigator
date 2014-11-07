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

import java.util.ArrayList;
import java.util.List;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class ObstaclesIndex {
	private final NodeAccess nodeAccess;
	private QuadTree quadtree;

	private List<Point> points = new ArrayList<Point>();

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

	public void queryByEdge(EdgeIteratorState edge, final double radius, final Function<Point> func) {
		if (!isInitialized)
			throw new IllegalArgumentException("ObstaclesIndex not initialized");

		final double aLat = nodeAccess.getLat(edge.getBaseNode());
		final double aLon = nodeAccess.getLon(edge.getBaseNode());
		final double bLat = nodeAccess.getLat(edge.getAdjNode());
		final double bLon = nodeAccess.getLon(edge.getAdjNode());

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

		quadtree.navigate(quadtree.getRootNode(), new Func() {
			@Override
			public void call(QuadTree quadTree, Node node) {
				double nodeLat = node.getX();
				double nodeLon = node.getY();

				double dist = Utils.calcDist(nodeLat, nodeLon,
						aLat, aLon, bLat, bLon, outInt, outDouble);

				if (dist < radius) {
					func.call((Point) node.getPoint().getValue());
				}
			}
		}, rectALat, rectALon, rectBLat, rectBLon);
	}

	public List<Point> queryByEdge(EdgeIteratorState edge, final double radius) {
		final ArrayList<Point> ret = new ArrayList<Point>();

		queryByEdge(edge, radius, new Function<Point>() {
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
		queryByEdge(edge, radius, new Function<Point>() {
			@Override
			public void call(Point point) {
				if (!half || point.getDifficulty() >= 5) {
					result[0] = true;
				}
			}
		});

		cacheProcessed.add(edgeId);

		if (result[0])
			cacheBlocked.add(edgeId);

		return result[0];
	}
}
