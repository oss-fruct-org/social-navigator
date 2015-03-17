package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.settings.Preferences;
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
		implements ItemizedIconOverlay.OnItemGestureListener<ObstaclesOverlayFragment.Obstacle>,
		RoutingService.Listener, PointsService.Listener,
		SharedPreferences.OnSharedPreferenceChangeListener {
	private static final Logger log = LoggerFactory.getLogger(ObstaclesOverlayFragment.class);

	public static final int POINT_UPDATE_INTERVAL = 60 * 3600;
	public static final int POINT_UPDATE_DISTANCE = 1000;
	public static final String PREF_LAST_UPDATE = "pref_last_update";

	private RoutingServiceConnection routingServiceConnection;
	private PointsServiceConnection pointsServiceConnection;

	private Drawable obstacleDrawable;
	private Drawable obstacleDrawablePrivate;

	private RoutingService routingService;
	private PointsService pointsService;

	private ItemizedIconOverlay<Obstacle> pathObstaclesOverlay;
	private ItemizedIconOverlay<Obstacle> privateObstaclesOverlay;

	private MapView mapView;
	private Preferences appPreferences;

	private Map<RoutingType, ChoicePath> paths;
	private AsyncTask<Void, Void, List<Obstacle>> privateObstaclesTask;
	private RoutingService.State routingState;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);
		obstacleDrawable = getActivity().getResources().getDrawable(R.drawable.blast);
		obstacleDrawablePrivate = getActivity().getResources().getDrawable(R.drawable.blast2);

		appPreferences = new Preferences(getActivity());
		appPreferences.getPref().registerOnSharedPreferenceChangeListener(this);

		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy() {
		appPreferences.getPref().unregisterOnSharedPreferenceChangeListener(this);

		if (privateObstaclesTask != null) {
			privateObstaclesTask.cancel(false);
		}

		if (routingService != null) {
			routingService.removeListener(this);
		}

		if (pointsService != null) {
			pointsService.removeListener(this);
		}

		if (routingServiceConnection != null) {
			getActivity().unbindService(routingServiceConnection);
		}

		if (pointsServiceConnection != null) {
			getActivity().unbindService(pointsServiceConnection);
		}

		super.onDestroy();
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		pathObstaclesOverlay = new ItemizedIconOverlay<>(new ArrayList<Obstacle>(),
				this, new DefaultResourceProxyImpl(getActivity()));
		mapView.getOverlayManager().add(pathObstaclesOverlay);

		privateObstaclesOverlay = new ItemizedIconOverlay<>(new ArrayList<Obstacle>(),
				this, new DefaultResourceProxyImpl(getActivity()));
		mapView.getOverlayManager().add(privateObstaclesOverlay);

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection = new RoutingServiceConnection(), Context.BIND_AUTO_CREATE);

		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection = new PointsServiceConnection(), Context.BIND_AUTO_CREATE);

		this.mapView = mapView;

		updatePrivateObstaclesOverlay();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			Toast.makeText(getActivity(), R.string.str_refreshing_obstacles, Toast.LENGTH_SHORT).show();
			if (routingService != null && pointsService != null) {
				Location lastLocation = routingService.getLastLocation();
				if (lastLocation != null) {
					pointsService.refresh(new GeoPoint(lastLocation));
				}
			}
		}

		return super.onOptionsItemSelected(item);
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

		if (pointsService != null) {
			autoUpdatePoints();
		}
	}

	private void onRoutingServiceDisconnected() {
		routingService = null;
	}

	private void onPointsServiceConnected(PointsService service) {
		pointsService = service;
		pointsService.addListener(this);

		if (routingService != null) {
			autoUpdatePoints();
		}
	}

	private void onPointsServiceDisconnected() {
		pointsService = null;
	}

	@Override
	public void routingStateChanged(RoutingService.State state) {
		routingState = state;
		if (state == RoutingService.State.IDLE) {
			pathObstaclesOverlay.removeAllItems();
			updatePrivateObstaclesOverlay();
		}
	}

	@Override
	public void progressStateChanged(boolean isActive) {
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
		this.paths = paths;
		RoutingType activeRoutingType = appPreferences.getActiveRoutingType();
		ChoicePath activePath = paths.get(activeRoutingType);
		updatePathObstaclesOverlay(activePath);
	}

	@Override
	public void activePathUpdated(RoutingService.TrackingState trackingState) {
		updatePathObstaclesOverlay(trackingState.initialPath);
	}

	@Override
	public void onDataUpdated(boolean isRemoteUpdate) {
		if (isRemoteUpdate) {
			long currentTime = System.currentTimeMillis();
			appPreferences.setLastPointsUpdateTimestamp(currentTime);
			appPreferences.setGeoPoint(PREF_LAST_UPDATE, new GeoPoint(routingService.getLastLocation()));
			Toast.makeText(getActivity(), R.string.str_data_refresh_complete, Toast.LENGTH_SHORT).show();
		}

		if (routingState == RoutingService.State.IDLE) {
			updatePrivateObstaclesOverlay();
		}
	}

	private void updatePrivateObstaclesOverlay() {
		if (pointsService == null) {
			return;
		}

		privateObstaclesTask = new AsyncTask<Void, Void, List<Obstacle>>() {
			@Override
			protected List<Obstacle> doInBackground(Void... params) {
				List<Point> points = pointsService.queryList(pointsService.requestPrivatePoints());
				List<Obstacle> obstacles = new ArrayList<>(points.size());

				for (Point point : points) {
					obstacles.add(new Obstacle(point.getName(), point.getDescription(),
							new GeoPoint(point.getLatE6(), point.getLonE6()), obstacleDrawablePrivate));
				}

				return obstacles;
			}

			@Override
			protected void onPostExecute(List<Obstacle> obstacles) {
				super.onPostExecute(obstacles);
				privateObstaclesOverlay.removeAllItems();
				privateObstaclesOverlay.addItems(obstacles);
				mapView.invalidate();
			}
		};

		privateObstaclesTask.execute();
	}

	private void clearPrivateObstaclesOverlay() {
		if (privateObstaclesTask != null) {
			privateObstaclesTask.cancel(false);
		}

		privateObstaclesOverlay.removeAllItems();
	}

	private void updatePathObstaclesOverlay(ChoicePath activePath) {
		if (activePath == null) {
			return;
		}

		clearPrivateObstaclesOverlay();
		pathObstaclesOverlay.removeAllItems();

		List<Obstacle> obstacles = new ArrayList<>(activePath.getPoints().length);
		for (Point point : activePath.getPoints()) {
			obstacles.add(new Obstacle(point.getName(), point.getDescription(),
					new GeoPoint(point.getLatE6(), point.getLonE6()), obstacleDrawable));
		}

		pathObstaclesOverlay.addItems(obstacles);
		mapView.invalidate();
	}

	private boolean autoUpdatePoints() {
		long lastUpdateTime = appPreferences.getLastPointsUpdateTimestamp();
		long currentTime = System.currentTimeMillis();

		Location lastLocation1 = routingService.getLastLocation();
		if (lastLocation1 == null) {
			return false;
		}

		GeoPoint currentLocation = new GeoPoint(lastLocation1);
		GeoPoint lastLocation = appPreferences.getGeoPoint(PREF_LAST_UPDATE);


		if (lastUpdateTime < 0 || currentTime - lastUpdateTime > POINT_UPDATE_INTERVAL
				|| lastLocation == null || currentLocation.distanceTo(lastLocation) > POINT_UPDATE_DISTANCE) {
			pointsService.refresh(currentLocation);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {
		Toast.makeText(getActivity(), R.string.str_data_refresh_failed, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.PREF_ACTIVE_ROUTING_TYPE)) {
			RoutingType activeRoutingType = appPreferences.getActiveRoutingType();
			if (paths != null) {
				ChoicePath activePath = paths.get(activeRoutingType);
				updatePathObstaclesOverlay(activePath);
			}
		}
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

	private class PointsServiceConnection implements ServiceConnection {
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
