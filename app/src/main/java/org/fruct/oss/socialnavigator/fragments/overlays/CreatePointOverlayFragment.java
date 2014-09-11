package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.Routing;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.PathOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreatePointOverlayFragment extends OverlayFragment implements PopupMenu.OnMenuItemClickListener {
	private static final Logger log = LoggerFactory.getLogger(CreatePointOverlayFragment.class);

	private EventOverlay overlay;

	private PathOverlay[] pathOverlays;

	private GeoPoint selectedPoint;
	private Routing routing;

	private AsyncTask<Void, Void, List<PointList>> routingTask;
	private MapView mapView;

	private PointsConnection pointsServiceConnection;
	private PointsService pointsService;
	private List<org.fruct.oss.socialnavigator.points.Point> obstaclesPoints;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (routingTask != null) {
			routingTask.cancel(true);
		}

		if (routing != null) {
			routing.close();
		}

		if (pointsServiceConnection != null) {
			getActivity().unbindService(pointsServiceConnection);
			pointsServiceConnection = null;
		}
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		Context context = getActivity();
		overlay = new EventOverlay(context);
		mapView.getOverlayManager().add(overlay);

		ResourceProxy proxy = new DefaultResourceProxyImpl(context);

		PathOverlay pathOverlay1 = new PathOverlay(0xaa33ff43, 4, proxy);
		PathOverlay pathOverlay2 = new PathOverlay(0xaa3343f4, 4, proxy);
		PathOverlay pathOverlay3 = new PathOverlay(0xaafe3443, 4, proxy);

		pathOverlays = new PathOverlay[] {pathOverlay1, pathOverlay2, pathOverlay3};
		for (PathOverlay pathOverlay : pathOverlays) {
			mapView.getOverlayManager().add(pathOverlay);
		}

		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection = new PointsConnection(), Context.BIND_AUTO_CREATE);
	}


	private void onPointsServiceConnected(PointsService service) {
		pointsService = service;

		obstaclesPoints = pointsService.queryList(pointsService.requestPoints(null));
	}

	private void onPointsServiceDisconnected() {
		pointsService = null;
	}

	private void onPointPressed(IGeoPoint geoPoint, Point screenPoint) {
		log.info("Map pressed on {}, {}", geoPoint.getLatitude(), geoPoint.getLongitude());

		View anchorView = getActivity().findViewById(R.id.map_anchor);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			anchorView.setTranslationX(screenPoint.x);
			anchorView.setTranslationY(screenPoint.y);
		}

		PopupMenu popupMenu = new PopupMenu(getActivity(), anchorView);
		popupMenu.inflate(R.menu.popup_point);
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();

		selectedPoint = new GeoPoint(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
	}

	private void route() {
		routingTask = new AsyncTask<Void, Void, List<PointList>>() {
			@Override
			protected List<PointList> doInBackground(Void... params) {
				if (routing == null) {
					routing = new Routing(getActivity(), "/sdcard/ptz.osm.pbf");
					routing.setObstacles(obstaclesPoints);
				}

				LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

				Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

				return routing.route(location.getLatitude(), location.getLongitude(),
						selectedPoint.getLatitude(), selectedPoint.getLongitude());
			}

			@Override
			protected void onPostExecute(List<PointList> pointLists) {
				super.onPostExecute(pointLists);

				int idx = 0;
				for (PathOverlay pathOverlay : pathOverlays) {
					pathOverlay.clearPath();
					PointList pointList = pointLists.get(idx++);

					for (int i = 0; i < pointList.size(); i++) {
						pathOverlay.addPoint((int) (pointList.getLatitude(i) * 1e6),
								(int) (pointList.getLongitude(i) * 1e6));
					}
				}
				mapView.invalidate();
			}
		}.execute();
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.action_route:
			route();
			break;

		case R.id.action_create:
			Toast.makeText(getActivity(), R.string.str_not_implemented, Toast.LENGTH_SHORT).show();
			break;

		default:
			return false;
		}

		return true;
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
			Projection projection = mapView.getProjection();
			IGeoPoint geoPoint = projection.fromPixels((int) e.getX(), (int) e.getY());

			Point screenPoint = new Point((int) e.getX(), (int) e.getY());
			screenPoint.offset(-projection.getScreenRect().left, -projection.getScreenRect().top);

			onPointPressed(geoPoint, screenPoint);
			return super.onLongPress(e, mapView);
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
}
