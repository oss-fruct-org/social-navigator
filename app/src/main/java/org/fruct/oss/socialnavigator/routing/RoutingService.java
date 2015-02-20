package org.fruct.oss.socialnavigator.routing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import com.graphhopper.util.PointList;

import org.fruct.oss.mapcontent.content.ContentManagerImpl;
import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.DirectoryContentItem;
import org.fruct.oss.mapcontent.content.ContentListenerAdapter;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnection;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnectionListener;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.mapcontent.content.ContentItem;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.NamedThreadFactory;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RoutingService extends Service implements PointsService.Listener,
		LocationReceiver.Listener, GeofencesManager.GeofencesListener,
		ContentServiceConnectionListener {
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	public static final String ACTION_ROUTE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_ROUTE";
	public static final String ACTION_PLACE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_PLACE";

	public static final String ARG_POINT = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_POINT";
	public static final String ARG_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_LOCATION";

	public static final String BC_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.BC_LOCATION";
	public static final int PROXIMITY_RADIUS = 20;

	public static final RoutingType[] REQUIRED_ROUTING_TYPES = {RoutingType.SAFE, RoutingType.NORMAL, RoutingType.FASTEST};

	private int GEOFENCE_TOKEN_OBSTACLES;
	private int GEOFENCE_TOKEN_INFO;

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("RoutingServiceThread"));

	// Tasks
	private Future<?> routeFuture;

	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Routing routing;

	private GeoPoint targetPoint;

	private final Map<RoutingType, Path> currentPathsMap = Collections.synchronizedMap(new EnumMap<RoutingType, Path>(RoutingType.class));
	private Turn currentTurn;
	private RoutingType currentRoutingType = RoutingType.SAFE;

	// Locks current target point ad current paths
	private final Object mutex = new Object();

	// Locks services assignment
	private final Object serviceMutex = new Object();

	private PointsServiceConnection pointsServiceConnection;
	private PointsService pointsService;

	private ContentServiceConnection contentServiceConnection = new ContentServiceConnection(this);
	private ContentService contentService;

	private LocationReceiver locationReceiver;
	private GeofencesManager geofencesManager;

	private Handler handler;

	private ContentItem recommendedContentItem;

	@Override
	public void onCreate() {
		super.onCreate();

		handler = new Handler(Looper.getMainLooper());

		locationReceiver = new LocationReceiver(this);
		locationReceiver.setListener(this);
		locationReceiver.start();

		geofencesManager = new SimpleGeofencesManager();
		geofencesManager.addListener(this);

		routing = new Routing();

		GEOFENCE_TOKEN_OBSTACLES = geofencesManager.createToken();
		GEOFENCE_TOKEN_INFO = geofencesManager.createToken();

		bindService(new Intent(this, PointsService.class),
				pointsServiceConnection = new PointsServiceConnection(), Context.BIND_AUTO_CREATE);
		//bindService(new Intent(this, RemoteContentService.class),
		//		remoteContentServiceConnection = new ContentServiceConnection(), BIND_AUTO_CREATE);
		//bindService(new Intent(this, DataService.class),
		//		dataServiceConnection = new DataServiceConnection(), BIND_AUTO_CREATE);

		currentPathsMap.clear();

		contentServiceConnection.bindService(this);

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

		if (pointsService != null) {
			pointsService.removeListener(this);
		}

		if (contentService != null) {
			contentService.removeListener(contentListener);
		}

		unbindService(pointsServiceConnection);
		pointsServiceConnection = null;

		locationReceiver.stop();
		locationReceiver.setListener(null);

		contentServiceConnection.unbindService(this);

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
			currentPathsMap.clear();
			targetPoint = null;
		}

		notifyPathsCleared();
	}

	public void sendLastResult() {
		synchronized (mutex) {
			notifyPathsUpdated(targetPoint, currentPathsMap);
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

		if (contentService != null) {
			contentService.setLocation(location);
		}
	}

	public Location getLastLocation() {
		return locationReceiver.getOldLocation();
	}

	private void checkGeofences(final Location location) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				geofencesManager.setLocation(location);
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

		final GeoPoint targetPoint = this.targetPoint;
		routeFuture = executor.submit(new Runnable() {
			@Override
			public void run() {
				if (!routing.isReady()) {
					return;
				}

				log.info("Starting paths calculation for point " + targetPoint.toString());
				for (RoutingType requiredRoutingType : REQUIRED_ROUTING_TYPES) {
					Path existingPath = currentPathsMap.get(requiredRoutingType);

					boolean isPathDeviated = true;
					if (existingPath != null && !forceRecalc) {
						existingPath.pointList.setLocation(currentLocation);
						isPathDeviated = existingPath.pointList.isDeviated();
					}

					if (existingPath == null || forceRecalc || isPathDeviated) {
						Path path = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
								targetPoint.getLatitude(), targetPoint.getLongitude(), requiredRoutingType);

						if (path != null) {
							currentPathsMap.put(requiredRoutingType, path);
						}
					}
				}

				Path activePath = currentPathsMap.get(currentRoutingType);
				if (activePath == null) {
					currentRoutingType = RoutingType.SAFE;
					activePath = currentPathsMap.get(currentRoutingType);
				}

				if (activePath != null) {
					if (activePath.getPointList().isCompleted()) {
						synchronized (mutex) {
							currentPathsMap.clear();
							RoutingService.this.targetPoint = null;
						}

						notifyPathsCleared();
					} else {
						updateActivePathWayInformation(activePath);
						notifyActivePathUpdated(activePath);
					}
				}

				if (!currentPathsMap.isEmpty()) {
					notifyPathsUpdated(targetPoint, currentPathsMap);
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
	private boolean initializeRouting(ContentItem contentItem) {
		try {
			String path = contentService.requestContentItem(contentItem);
			routing.loadFromPref(this, path);
			setObstaclesPoints();
		} catch (Exception ex) {
			return false;
		}

		return true;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private void setObstaclesPoints() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				List<Point> obstaclesPoints;
				synchronized (serviceMutex) {
					if (!routing.isReady() || pointsService == null) {
						return;
					}

					// Set obstacles
					obstaclesPoints = pointsService.queryList(pointsService.requestPoints());
					routing.setObstacles(obstaclesPoints);
				}

				recalculatePaths(true);

				// Set geofences
				geofencesManager.removeGeofences(GEOFENCE_TOKEN_OBSTACLES);
				for (Point point : obstaclesPoints) {
					Bundle data = new Bundle(1);
					data.putParcelable("point", point);

					geofencesManager.addGeofence(GEOFENCE_TOKEN_OBSTACLES, point.getLat(), point.getLon(), PROXIMITY_RADIUS, data);
				}
			}
		});
	}

	private void onPointsServiceReady(PointsService pointsService) {
		synchronized (serviceMutex) {
			this.pointsService = pointsService;
		}
		pointsService.addListener(this);
	}

	private void onPointsServiceDisconnected() {
		synchronized (serviceMutex) {
			this.pointsService = null;
		}
	}

	@Override
	public void onContentServiceReady(ContentService contentService) {
		synchronized (serviceMutex) {
			this.contentService = contentService;
			contentService.addListener(contentListener);
			contentService.setLocation(getLastLocation());
		}
	}

	@Override
	public void onContentServiceDisconnected() {
		synchronized (serviceMutex) {
			this.contentService = null;
		}
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

	private void notifyActivePathUpdated(final Path activePath) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.activePathUpdated(activePath);
				}
			}
		});
	}

	private void notifyPathsUpdated(final GeoPoint targetPoint, final Map<RoutingType, Path> paths) {
		if (targetPoint == null)
			return;

		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsUpdated(targetPoint, paths, currentRoutingType);
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

	private void notifyProximityEvent(final Point point) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.proximityEvent(point);
				}
			}
		});
	}

	private void notifyProximityEvent(final Turn turn) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.proximityEvent(turn);
				}
			}
		});
	}

	public void setRoutingTypeActive(RoutingType activeRoutingType) {
		this.currentRoutingType = activeRoutingType;
		Path activePath = currentPathsMap.get(currentRoutingType);
		updateActivePathWayInformation(activePath);
		notifyActivePathUpdated(activePath);
	}

	public void updateActivePathWayInformation(Path activePath) {
		Turn newTurn = activePath.getPointList().checkTurn();
		if (newTurn != null) {
			if (currentTurn == null || !currentTurn.equals(newTurn)) {
				currentTurn = newTurn;
				Bundle data = new Bundle(1);
				data.putParcelable("turn", newTurn);
				geofencesManager.removeGeofences(GEOFENCE_TOKEN_INFO);
				geofencesManager.addGeofence(GEOFENCE_TOKEN_INFO,
						newTurn.getPoint().x, newTurn.getPoint().y, PROXIMITY_RADIUS, data);
			}
		} else {
			geofencesManager.removeGeofences(GEOFENCE_TOKEN_INFO);
		}
	}

	@Override
	public void geofenceEntered(final Bundle data) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (data.containsKey("point")) {
					notifyProximityEvent((Point) data.getParcelable("point"));
				} else if (data.containsKey("turn")) {
					notifyProximityEvent((Turn) data.getParcelable("turn"));
				}
			}
		});
	}

	@Override
	public void geofenceExited(Bundle data) {
	}

	private ContentService.Listener contentListener = new ContentListenerAdapter() {
		@Override
		public void recommendedRegionItemReady(final ContentItem contentItem) {
			log.debug("Recommended content item received");

			if (!contentItem.getType().equals(ContentManagerImpl.GRAPHHOPPER_MAP)) {
				return;
			}

			if (recommendedContentItem == contentItem) {
				return;
			}

			recommendedContentItem = contentItem;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					initializeRouting(contentItem);
				}
			});
		}
	};

	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}

	public static class Path {
		private final Point[] points;
		private final PathPointList pointList;
		private final RoutingType routingType;
		private final double distance;

		public Path(PointList ghPointList, double distance, RoutingType routingType, Point[] points) {
			this.pointList = new PathPointList(ghPointList);
			this.distance = distance;
			this.routingType = routingType;
			this.points = points;
		}

		public PathPointList getPointList() {
			return pointList;
		}

		public double getDistance() {
			return distance;
		}

		public RoutingType getRoutingType() {
			return routingType;
		}

		public Point[] getPoints() {
			return points;
		}
	}

	public static interface Listener {
		void proximityEvent(Point point);
		void proximityEvent(Turn turn);
		void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, Path> paths, RoutingType activeType);
		void pathsCleared();
		void activePathUpdated(Path activePath);
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
