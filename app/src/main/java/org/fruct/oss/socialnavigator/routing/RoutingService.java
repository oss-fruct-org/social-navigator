package org.fruct.oss.socialnavigator.routing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import com.graphhopper.GHResponse;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RoutingService extends Service implements PointsService.Listener, LocationReceiver.Listener {
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	public static final String ACTION_ROUTE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_ROUTE";
	public static final String ACTION_PLACE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_PLACE";

	public static final String ARG_POINT = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_POINT";
	public static final String ARG_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_LOCATION";

	public static final String BC_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.BC_LOCATION";

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private LocationManager locationManager;

	private final Object mutex = new Object();

	// Tasks
	private Future<Routing> initializeFuture;
	private Future<List<Path>> routeFuture;

	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Routing routing;

	private GeoPoint targetPoint;
	private List<Path> currentRoutes;
	private Path activePath;


	private PointsServiceConnection pointsServiceConnection;
	private PointsService pointsService;

	private LocationReceiver locationReceiver;

	private Handler handler;

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler(Looper.getMainLooper());

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		initializeFuture = executor.submit(new Callable<Routing>() {
			@Override
			public Routing call() throws Exception {
				return new Routing("/sdcard/ptz.osm.pbf");
			}
		});

		locationReceiver = new LocationReceiver(this);
		locationReceiver.setListener(this);
		locationReceiver.start();

		bindService(new Intent(this, PointsService.class),
				pointsServiceConnection = new PointsServiceConnection(), Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		executor.shutdown();

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					executor.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (routing != null) {
					routing.close();
				}

				return null;
			}
		}.execute();

		unbindService(pointsServiceConnection);
		pointsServiceConnection = null;

		if (pointsService != null) {
			pointsService.removeListener(this);
		}

		locationReceiver.stop();
		locationReceiver.setListener(null);

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return RoutingService.START_NOT_STICKY;
		}

		String action = intent.getAction();
		if (action.equals(ACTION_ROUTE)) {
			GeoPoint targetPoint = intent.getParcelableExtra(ARG_POINT);
			if (targetPoint == null) {
				clearTargetPoint();
			} else {
				newTargetPoint(targetPoint);
			}
		} else if (action.equals(ACTION_PLACE)) {
			GeoPoint targetPoint = intent.getParcelableExtra(ARG_POINT);
			setCurrentLocation(targetPoint);
		}

		return RoutingService.START_NOT_STICKY;
	}

	private void clearTargetPoint() {
		if (routeFuture != null) {
			routeFuture.cancel(true);
		}

		synchronized (mutex) {
			currentRoutes = null;
			targetPoint = null;
		}

		notifyPathsCleared();
	}

	public void setPathActive(Path path) {
		synchronized (mutex) {
			activePath = path;
			currentRoutes = Collections.emptyList();
		}

		notifyRoutingUpdated(path);
	}

	public void sendLastResult() {
		synchronized (mutex) {
			if (currentRoutes != null) {
				notifyPathsUpdated(currentRoutes);
			}
		}
	}

	public void sendLastLocation() {
		locationReceiver.sendLastLocation();
	}

	public void setCurrentLocation(GeoPoint currentLocation) {
		if (locationReceiver == null)
			return;

		Location location = new Location("test-provider");
		location.setLatitude(currentLocation.getLatitude());
		location.setLongitude(currentLocation.getLongitude());
		location.setTime(System.currentTimeMillis());
		location.setAccuracy(1);

		if (Build.VERSION.SDK_INT >= 17) {
			location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
		}

		locationReceiver.mockLocation(location);
	}

	@Override
	public void newLocation(Location location) {
		Intent intent = new Intent(BC_LOCATION);
		intent.putExtra(ARG_LOCATION, location);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		recalculateActivePath(location);
	}

	private void recalculateActivePath(final Location location) {
		final Path path;
		synchronized (mutex) {
			if (activePath == null) {
				return;
			} else {
				path = activePath;
			}
		}

		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (!ensureRoutingReady()) {
					// TODO: notify user that routing haven't been initialized
					return;
				}

				Path newPath = routing.route(location.getLatitude(), location.getLongitude(),
						targetPoint.getLatitude(), targetPoint.getLongitude(),
						path.vehicle, path.weighting);

				synchronized (mutex) {
					activePath = newPath;
					notifyRoutingUpdated(activePath);
				}
			}
		});
	}

	private void newTargetPoint(final GeoPoint targetPoint) {
		if (routeFuture != null) {
			routeFuture.cancel(true);
		}

		final Location currentLocation = locationReceiver.getOldLocation();
		if (currentLocation == null) {
			return;
		}

		this.targetPoint = targetPoint;
		routeFuture = executor.submit(new Callable<List<Path>>() {
			@Override
			public List<Path> call() {
				if (!ensureRoutingReady()) {
					// TODO: notify user that routing haven't been initialized
					return null;
				}

				List<Path> newRoutes = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
						targetPoint.getLatitude(), targetPoint.getLongitude());

				if (newRoutes.size() > 0) {
					newRoutes.get(0).setActive(true);
				}

				synchronized (mutex) {
					if (Thread.currentThread().isInterrupted())
						return null;

					currentRoutes = newRoutes;
					notifyPathsUpdated(currentRoutes);
				}

				return newRoutes;
			}
		});
	}

	@Blocking
	private boolean ensureRoutingReady() {
		if (initializeFuture != null) {
			try {
				routing = initializeFuture.get();
			} catch (InterruptedException e) {
				return false;
			} catch (ExecutionException e) {
				return false;
			} catch (CancellationException e) {
				return false;
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private void setObstaclesPoints() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (!ensureRoutingReady()) {
					return;
				}

				List<Point> obstaclesPoints = pointsService.queryList(pointsService.requestPoints(null));
				routing.setObstacles(obstaclesPoints);
			}
		});
	}

	private void onPointsServiceReady(PointsService pointsService) {
		this.pointsService = pointsService;

		pointsService.addListener(this);

		setObstaclesPoints();
	}

	private void onPointsServiceDisconnected() {
		this.pointsService = null;
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	@Override
	public void onDataUpdated() {
		setObstaclesPoints();
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {
	}

	private void notifyPathsUpdated(final List<Path> results) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsUpdated(results);
				}
			}
		});
	}

	private void notifyPathsCleared() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsCleared();
				}
			}
		});
	}

	private void notifyRoutingUpdated(final Path path) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.routingUpdated(path);
				}
			}
		});
	}


	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}

	public static class Path {
		private final GHResponse response;
		private final PointList pointList;
		private final String vehicle;
		private final String weighting;
		private boolean isActive;

		Path(GHResponse response, String vehicle, String weighting) {
			this.pointList = response.getPoints();
			this.response = response;
			this.vehicle = vehicle;
			this.weighting = weighting;
		}

		void setActive(boolean isActive) {
			this.isActive = isActive;
		}

		public PointList getPointList() {
			return pointList;
		}

		public String getVehicle() {
			return vehicle;
		}

		public String getWeighting() {
			return weighting;
		}

		public GHResponse getResponse() {
			return response;
		}

		public boolean isActive() {
			return isActive;
		}
	}

	public static interface Listener {
		void pathsUpdated(List<Path> paths);
		void pathsCleared();
		void routingUpdated(Path path);
	}

	private class PointsServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			PointsService pointsService = ((PointsService.Binder) service).getService();
			onPointsServiceReady(pointsService);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onPointsServiceDisconnected();
		}
	}
}
