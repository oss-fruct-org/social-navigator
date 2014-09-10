package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MotionEvent;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ObstaclesOverlayFragment extends OverlayFragment
		implements ItemizedIconOverlay.OnItemGestureListener<ObstaclesOverlayFragment.Obstacle>,PointsService.Listener {
	private static final Logger log = LoggerFactory.getLogger(ObstaclesOverlayFragment.class);

	private PointsConnection pointsServiceConnection;
	private Drawable obstacleDrawable;
	private PointsService pointsService;
	private ItemizedIconOverlay<Obstacle> overlay;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		obstacleDrawable = getActivity().getResources().getDrawable(R.drawable.blast);
	}

	@Override
	public void onDestroy() {
		if (pointsService != null) {
			pointsService.removeListener(this);
		}

		getActivity().unbindService(pointsServiceConnection);
		pointsServiceConnection = null;
		super.onDestroy();
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		overlay = new ItemizedIconOverlay<Obstacle>(new ArrayList<Obstacle>(), this, new DefaultResourceProxyImpl(getActivity()));
		mapView.getOverlayManager().add(overlay);

		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection = new PointsConnection(), Context.BIND_AUTO_CREATE);
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

	private void onPointsServiceConnected(PointsService service) {
		pointsService = service;
		pointsService.addListener(this);

		onDataUpdated();
	}

	private void onPointsServiceDisconnected() {
		pointsService = null;
	}

	@Override
	public void onDataUpdated() {
		List<Point> points = pointsService.queryList(pointsService.requestPoints(null));
		overlay.removeAllItems();

		List<Obstacle> obstacles = new ArrayList<Obstacle>(points.size());
		for (Point point : points) {
			obstacles.add(new Obstacle(point.getName(), point.getDescription(),
					new GeoPoint(point.getLatE6(), point.getLonE6()), obstacleDrawable));
		}
		overlay.addItems(obstacles);
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {

	}

	public static class Obstacle extends OverlayItem {
		public Obstacle(String aTitle, String aSnippet, GeoPoint aGeoPoint, Drawable drawable) {
			super(aTitle, aSnippet, aGeoPoint);
			setMarker(drawable);
		}
	}

	private class PointsConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (pointsServiceConnection != null) {
				onPointsServiceConnected(((PointsService.Binder) service).getService());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onPointsServiceDisconnected();
		}
	}


	private class EventOverlay extends Overlay {
		public EventOverlay(Context ctx) {
			super(ctx);
		}

		@Override
		protected void draw(Canvas c, MapView osmv, boolean shadow) {
		}

		@Override
		public boolean onLongPress(MotionEvent e, MapView mapView) {
			Projection proj = mapView.getProjection();
			IGeoPoint point = proj.fromPixels((int) e.getX(), (int) e.getY());

			return super.onLongPress(e, mapView);
		}
	}
}
