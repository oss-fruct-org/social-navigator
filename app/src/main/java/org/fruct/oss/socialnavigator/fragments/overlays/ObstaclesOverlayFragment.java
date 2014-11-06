package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ObstaclesOverlayFragment extends OverlayFragment
		implements ItemizedIconOverlay.OnItemGestureListener<ObstaclesOverlayFragment.Obstacle>, RoutingService.Listener {
	private static final Logger log = LoggerFactory.getLogger(ObstaclesOverlayFragment.class);

	private RoutingServiceConnection routingServiceConnection;

	private Drawable obstacleDrawable;
	private RoutingService routingService;
	private ItemizedIconOverlay<Obstacle> overlay;
	private MapView mapView;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);
		obstacleDrawable = getActivity().getResources().getDrawable(R.drawable.blast);
	}

	@Override
	public void onDestroy() {
		if (routingService != null) {
			routingService.removeListener(this);
		}

		getActivity().unbindService(routingServiceConnection);
		super.onDestroy();
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		overlay = new ItemizedIconOverlay<Obstacle>(new ArrayList<Obstacle>(), this, new DefaultResourceProxyImpl(getActivity()));
		mapView.getOverlayManager().add(overlay);

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection = new RoutingServiceConnection(), Context.BIND_AUTO_CREATE);
		this.mapView = mapView;
	}

	@Override
	public boolean onItemSingleTapUp(int index, Obstacle item) {
		log.debug("Item pressed");
		return true;
	}

	@Override
	public boolean onItemLongPress(int index, Obstacle item) {
		return false;
	}

	private void onRoutingServiceConnected(RoutingService service) {
		routingService = service;
		routingService.addListener(this);
	}

	private void onRoutingServiceDisconnected() {
		routingService = null;
	}

	/*@Override
	public void onDataUpdated() {
		List<Point> points = pointsService.queryList(pointsService.requestPoints());
		overlay.removeAllItems();

		List<Obstacle> obstacles = new ArrayList<Obstacle>(points.size());
		for (Point point : points) {
			obstacles.add(new Obstacle(point.getName(), point.getDescription(),
					new GeoPoint(point.getLatE6(), point.getLonE6()), obstacleDrawable));
		}
		overlay.addItems(obstacles);
	}*/

	@Override
	public void proximityEvent(Point point) {
	}

	@Override
	public void proximityEvent(Turn turn) {
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, RoutingService.Path> paths, RoutingType activeType) {
	}

	@Override
	public void pathsCleared() {
		overlay.removeAllItems();
	}

	@Override
	public void activePathUpdated(RoutingService.Path activePath) {
		overlay.removeAllItems();

		List<Obstacle> obstacles = new ArrayList<Obstacle>(activePath.getPoints().length);
		for (Point point : activePath.getPoints()) {
			obstacles.add(new Obstacle(point.getName(), point.getDescription(),
					new GeoPoint(point.getLatE6(), point.getLonE6()), obstacleDrawable));
		}

		overlay.addItems(obstacles);
		mapView.invalidate();
	}

	public static class Obstacle extends OverlayItem {
		public Obstacle(String aTitle, String aSnippet, GeoPoint aGeoPoint, Drawable drawable) {
			super(aTitle, aSnippet, aGeoPoint);
			setMarker(drawable);
		}
	}

	private class RoutingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (routingServiceConnection != null) {
				onRoutingServiceConnected(((RoutingService.Binder) service).getService());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onRoutingServiceDisconnected();
		}
	}

}
