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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.graphhopper.GHResponse;
import com.graphhopper.util.Instruction;

import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RoutingService extends Service implements PointsService.Listener, LocationReceiver.Listener, GeofencesManager.GeofencesListener {
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	public static final String ACTION_ROUTE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_ROUTE";
	public static final String ACTION_PLACE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_PLACE";
	public static final String ACTION_GEO_FENCE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_GEOFENCE";

	public static final String ARG_POINT = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_POINT";
	public static final String ARG_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_LOCATION";

	public static final String BC_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.BC_LOCATION";
	public static final int END_ROUTE_DISTANCE = 20;
	public static final int PROXIMITY_RADIUS = 20;

	private int GEOFENCE_TOKEN_OBSTACLES;
	private int GEOFENCE_TOKEN_INFO;

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private LocationManager locationManager;

	private final Object mutex = new Object();

	// Tasks
	private Future<Routing> initializeFuture;
	private Future<?> routeFuture;

	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Routing routing;

	private GeoPoint targetPoint;
	private List<Path> currentPaths;

	private PointsServiceConnection pointsServiceConnection;
	private PointsService pointsService;

	private LocationReceiver locationReceiver;
	private GeofencesManager geofencesManager;

	private Handler handler;

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler(Looper.getMainLooper());

		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		initializeFuture = executor.submit(new Callable<Routing>() {
			@Override
			public Routing call() throws Exception {
				Routing routing = new Routing();
				routing.loadFromAsset(RoutingService.this, "map.osm.pbf.ghz", 1);
				return routing;
			}
		});

		locationReceiver = new LocationReceiver(this);
		locationReceiver.setListener(this);
		locationReceiver.start();

		geofencesManager = new SimpleGeofencesManager();
		geofencesManager.addListener(this);

		GEOFENCE_TOKEN_OBSTACLES = geofencesManager.createToken();
		GEOFENCE_TOKEN_INFO = geofencesManager.createToken();

		bindService(new Intent(this, PointsService.class),
				pointsServiceConnection = new PointsServiceConnection(), Context.BIND_AUTO_CREATE);
		log.info("created");
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

		log.info("destroyed");
		super.onDestroy();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		handler.postDelayed(stopRunnable, 10000);
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		handler.removeCallbacks(stopRunnable);
	}

	private Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			stopSelf();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return RoutingService.START_NOT_STICKY;
		}

		handler.removeCallbacks(stopRunnable);

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
			currentPaths = null;
			targetPoint = null;
		}

		notifyPathsCleared();
	}

	public void sendLastResult() {
		synchronized (mutex) {
			if (currentPaths != null) {
				notifyPathsUpdated(targetPoint, currentPaths);
			}
		}
	}

	public void sendLastLocation() {
		locationReceiver.sendLastLocation();
	}

	public void setCurrentLocation(GeoPoint currentLocation) {
		if (locationReceiver == null)
			return;

		Location location = new Location(LocationReceiver.MOCK_PROVIDER);
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

		checkGeofences(location);
		recalculatePaths(false);
	}

	private void checkGeofences(final Location location) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (geofencesManager) {
					geofencesManager.setLocation(location);
				}
			}
		});
	}

	private void recalculatePaths(final boolean forceRecalc) {
		if (routeFuture != null) {
			routeFuture.cancel(true);
		}

		if (targetPoint == null) {
			log.warn("Trying to recalculate paths with null target point");
			return;
		}

		final Location currentLocation = locationReceiver.getOldLocation();
		if (currentLocation == null) {
			log.warn("No location");
			return;
		}

		if (new GeoPoint(currentLocation).distanceTo(targetPoint) < END_ROUTE_DISTANCE) {
			clearTargetPoint();
			return;
		}

		final GeoPoint targetPoint = this.targetPoint;
		routeFuture = executor.submit(new Runnable() {
			@Override
			public void run() {
				if (!ensureRoutingReady()) {
					// TODO: notify user that routing haven't been initialized
					return;
				}

				if (currentPaths == null) {
					// No current path set
					List<Path> newRoutes = null;
					try {
						newRoutes = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
								targetPoint.getLatitude(), targetPoint.getLongitude());
					} catch (Exception ex) {
						log.error("Can't find path", ex);
						return;
					}

					if (newRoutes.size() > 0) {
						newRoutes.get(0).setActive(true);
						updateActivePathWayInformation(newRoutes.get(0));
					}

					synchronized (mutex) {
						if (Thread.currentThread().isInterrupted())
							return;

						currentPaths = newRoutes;
						notifyPathsUpdated(targetPoint, currentPaths);
					}
				} else if (forceRecalc) {
					// Existing path set and new target point

					for (Path path : currentPaths) {
						Path newPath = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
								targetPoint.getLatitude(), targetPoint.getLongitude(),
								path.getRoutingType());
						path.pointList = newPath.getPointList();
						path.response = newPath.getResponse();

						if (path.isActive()) {
							updateActivePathWayInformation(path);
						}
					}

					synchronized (mutex) {
						if (Thread.currentThread().isInterrupted())
							return;

						notifyPathsUpdated(targetPoint, currentPaths);
					}
				} else {
					// New location with existing path set and same target point
					for (Path path : currentPaths) {
						path.pointList.setLocation(currentLocation);
						if (path.pointList.isDeviated()) {
							Path newPath = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
									targetPoint.getLatitude(), targetPoint.getLongitude(),
									path.getRoutingType());
							path.pointList = newPath.getPointList();
							path.response = newPath.getResponse();
						}
					}

					synchronized (mutex) {
						if (Thread.currentThread().isInterrupted())
							return;

						notifyPathsUpdated(targetPoint, currentPaths);
					}
				}
			}
		});
	}

	public void newTargetPoint(final GeoPoint targetPoint) {
		if (targetPoint == null) {
			clearTargetPoint();
			return;
		}

		this.targetPoint = targetPoint;

		recalculatePaths(true);
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

				// Set obstacles
				List<Point> obstaclesPoints = pointsService.queryList(pointsService.requestPoints(null));
				routing.setObstacles(obstaclesPoints);

				// Set geofences
				synchronized (geofencesManager) {
					geofencesManager.removeGeofences(GEOFENCE_TOKEN_OBSTACLES);
					for (Point point : obstaclesPoints) {
						Bundle data = new Bundle(1);
						data.putParcelable("point", point);

						geofencesManager.addGeofence(GEOFENCE_TOKEN_OBSTACLES, point.getLat(), point.getLon(), PROXIMITY_RADIUS, data);
					}
				}
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

	private void notifyPathsUpdated(final GeoPoint targetPoint, final List<Path> results) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsUpdated(targetPoint, results);
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

	public void setPathActive(Path currentPath) {
		synchronized (mutex) {
			for (Path path : currentPaths) {
				if (path.equals(currentPath)) {
					path.setActive(true);
				} else {
					path.setActive(false);
				}
			}
		}

		updateActivePathWayInformation(currentPath);
	}

	public void updateActivePathWayInformation(Path activePath) {
		synchronized (geofencesManager) {
			geofencesManager.removeGeofences(GEOFENCE_TOKEN_INFO);

			for (Turn turn : Utils.findTurns(Utils.toList(activePath.getResponse().getPoints()))) {
				geofencesManager.addGeofence(GEOFENCE_TOKEN_INFO, turn.getGeoPoint().getLatitude(), turn.getGeoPoint().getLongitude(), PROXIMITY_RADIUS, new Bundle());
			}
		}
	}

	@Override
	public void geofenceEntered(final Bundle data) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (data.containsKey("point")) {
					Toast.makeText(RoutingService.this, "Geofence " + ((Point) data.getParcelable("point")).getName(), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(RoutingService.this, "Geofence instruction " + data.getString("name") + " " + data.getInt("type"),
							Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	@Override
	public void geofenceExited(Bundle data) {
	}

	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}

	public static class Path {
		private GHResponse response;
		private PathPointList pointList;

		private final String vehicle;
		private final String weighting;
		private boolean isActive;
		private RoutingType routingType;

		public Path(GHResponse response, RoutingType routingType) {
			this.pointList = new PathPointList(response);
			this.response = response;
			this.routingType = routingType;
			this.vehicle = routingType.getVehicle();
			this.weighting = routingType.getWeighting();
		}

		void setActive(boolean isActive) {
			this.isActive = isActive;
		}

		public PathPointList getPointList() {
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

		public RoutingType getRoutingType() {
			return routingType;
		}
	}

	public static interface Listener {
		void pathsUpdated(GeoPoint targetPoint, List<Path> paths);
		void pathsCleared();
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
