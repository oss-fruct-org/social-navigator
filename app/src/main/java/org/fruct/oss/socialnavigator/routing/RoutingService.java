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

import org.fruct.oss.mapcontent.BuildConfig;
import org.fruct.oss.mapcontent.content.ContentManagerImpl;
import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.ContentListenerAdapter;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnection;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnectionListener;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.mapcontent.content.ContentItem;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.EarthSpace;
import org.fruct.oss.socialnavigator.utils.NamedThreadFactory;
import org.fruct.oss.socialnavigator.utils.Space;
import org.fruct.oss.socialnavigator.utils.Timer;
import org.fruct.oss.socialnavigator.utils.TrackPath;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RoutingService extends Service implements PointsService.Listener,
		LocationReceiver.Listener, GeofencesManager.GeofencesListener,
		ContentServiceConnectionListener {
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	public static final String ARG_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_LOCATION";

	public static final String BC_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.BC_LOCATION";
	public static final int PROXIMITY_RADIUS = 20;

	public static final RoutingType[] REQUIRED_ROUTING_TYPES = {
			RoutingType.SAFE,
			RoutingType.NORMAL,
			RoutingType.FASTEST};

	private int GEOFENCE_TOKEN_OBSTACLES;
	private int GEOFENCE_TOKEN_INFO;

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory("RoutingServiceThread"));

	// Tasks
	private Future<?> routeFuture;

	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Routing routing;

	// Locks current target point ad current paths
	private final Object mutex = new Object();

	private final Map<RoutingType, ChoicePath> currentPathsMap = Collections.synchronizedMap(new EnumMap<RoutingType, ChoicePath>(RoutingType.class));
	private Turn currentTurn;

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


	// Variables that represent routing and must be persistent
	private GeoPoint currentLocation;
	private GeoPoint targetLocation;
	private State state;
	private TrackingState trackingState;

	@Override
	public void onCreate() {
		super.onCreate();

		state = State.IDLE;

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
		handler.removeCallbacks(stopRunnable);

		return RoutingService.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}


	public void startTracking(RoutingType routingType) {
		ChoicePath activePath = currentPathsMap.get(routingType);

		List<Space.Point> points = new ArrayList<Space.Point>();

		PointList ghPointList = activePath.getResponse().getPoints();
		for (int i = 0; i < ghPointList.size(); i++) {
			points.add(new Space.Point(ghPointList.getLat(i), ghPointList.getLon(i)));
		}

		trackingState = new TrackingState();
		trackingState.initialPath = activePath;
		trackingState.trackingPath = new TrackPath<Point>(new EarthSpace(), points);

		Point[] pointsOnPath = activePath.getPoints();
		for (Point point : pointsOnPath) {
			trackingState.trackingPath.addPoint(point.getLat(), point.getLon(), point);
		}

		trackingState.lastQueryResult = trackingState.trackingPath.query(
				currentLocation.getLatitude(),
				currentLocation.getLongitude());

		changeRoutingState(State.TRACKING);
		notifyActivePathUpdated(trackingState);
	}

	public void stopTracking() {
		trackingState = null;
		changeRoutingState(State.IDLE);
		recalculatePaths();
	}

	public void setTargetPoint(GeoPoint targetPoint) {
		this.targetLocation = targetPoint;

		if (state == State.IDLE || state == State.CHOICE || state == State.UPDATING) {
			recalculatePaths();
		}
	}

	public void clearTargetPoint() {
		synchronized (mutex) {
			if (routeFuture != null) {
				routeFuture.cancel(true);
			}

			currentPathsMap.clear();
			targetLocation = null;
		}

		changeRoutingState(State.IDLE);
		notifyPathsCleared();
	}

	public void sendLastResult() {
		synchronized (mutex) {
			if (state == State.UPDATING || state == State.CHOICE) {
				notifyPathsUpdated(targetLocation, currentPathsMap);
			} else if (state == State.TRACKING) {
				notifyActivePathUpdated(trackingState);
			}

			notifyRoutingStateChanged(state);
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

		// TODO: this should be replaced with new obstacles system
		checkGeofences(location);

		if (state == State.CHOICE) {
			recalculatePaths();
		} else if (state == State.TRACKING) {
			updateActivePath(location);
		}

		if (contentService != null) {
			contentService.setLocation(location);
		}
	}

	public Location getLastLocation() {
		return locationReceiver.getOldLocation();
	}

	private void updateActivePath(Location location) {
		Timer timer = new Timer().start();
		trackingState.lastQueryResult = trackingState.trackingPath.query(
				location.getLatitude(), location.getLongitude());
		log.debug("Updating active path took {} seconds", timer.stop().getSeconds());

		notifyActivePathUpdated(trackingState);
	}

	private void checkGeofences(final Location location) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				geofencesManager.setLocation(location);
			}
		});
	}

	private void recalculatePaths() {
		synchronized (mutex) {
			if (routeFuture != null) {
				routeFuture.cancel(true);
			}

			if (targetLocation == null) {
				log.warn("Trying to recalculate paths with null target point");
				return;
			}

			final Location currentLocation = locationReceiver.getOldLocation();
			if (currentLocation == null) {
				log.warn("No location");
				return;
			}

			final GeoPoint targetPoint = this.targetLocation;

			routeFuture = executor.submit(new Runnable() {
				@Override
				public void run() {
					if (!routing.isReady()) {
						return;
					}

					changeRoutingState(State.UPDATING);
					log.info("Starting paths calculation for point " + targetPoint.toString());
					for (RoutingType requiredRoutingType : REQUIRED_ROUTING_TYPES) {
						/*if (existingPath != null && !forceRecalc) {
							existingPath.getPointList().setLocation(currentLocation);
							isPathDeviated = existingPath.getPointList().isDeviated();
						}*/

						ChoicePath path = routing.route(currentLocation.getLatitude(),
								currentLocation.getLongitude(),
								targetPoint.getLatitude(),
								targetPoint.getLongitude(),
								requiredRoutingType);

						if (path != null) {
							currentPathsMap.put(requiredRoutingType, path);
						} else {
							currentPathsMap.remove(requiredRoutingType);
						}
					}

					// FIXME: don't notify if service already in another state
					RoutingService.this.currentLocation = new GeoPoint(getLastLocation());
					if (!currentPathsMap.isEmpty()) {
						notifyPathsUpdated(targetPoint, currentPathsMap);
						changeRoutingState(State.CHOICE);
					} else {
						changeRoutingState(State.IDLE);
					}
				}
			});
		}
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

				if (state == State.CHOICE || state == State.UPDATING) {
					recalculatePaths();
				}

				// Set geofences
				/*geofencesManager.removeGeofences(GEOFENCE_TOKEN_OBSTACLES);
				for (Point point : obstaclesPoints) {
					Bundle data = new Bundle(1);
					data.putParcelable("point", point);

					geofencesManager.addGeofence(GEOFENCE_TOKEN_OBSTACLES, point.getLat(), point.getLon(), PROXIMITY_RADIUS, data);
				}*/
			}
		});
	}

	private void changeRoutingState(State state) {
		if (BuildConfig.DEBUG && !this.state.checkTransition(state)) {
			throw new IllegalStateException("Can't change state from " + this.state + " to " + state);
		}

		this.state = state;
		notifyRoutingStateChanged(state);
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

	private void notifyRoutingStateChanged(final State state) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.routingStateChanged(state);
				}
			}
		});
	}

	private void notifyPathsUpdated(final GeoPoint targetPoint, final Map<RoutingType, ChoicePath> paths) {
		if (targetPoint == null)
			return;

		final Map<RoutingType, ChoicePath> pathMapCopy = new HashMap<RoutingType, ChoicePath>(paths);

		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsUpdated(targetPoint, pathMapCopy);
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

	private void notifyActivePathUpdated(final TrackingState trackingState) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.activePathUpdated(trackingState);
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

	/*
		public void setRoutingTypeActive(RoutingType activeRoutingType) {
			this.currentRoutingType = activeRoutingType;
			ChoicePath activePath = currentPathsMap.get(currentRoutingType);
			updateActivePathWayInformation(activePath);
			synchronized (mutex) {
				notifyPathsUpdated(targetLocation, currentPathsMap);
			}
		}

		public void updateActivePathWayInformation(ChoicePath activePath) {
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
	*/
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

		@Override
		public void requestContentReload() {
			if (recommendedContentItem == null) {
				return;
			}

			executor.execute(new Runnable() {
				@Override
				public void run() {
					initializeRouting(recommendedContentItem);
				}
			});
		}
	};

	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}

	public static interface Listener {
		void proximityEvent(Point point);
		void proximityEvent(Turn turn);
		void routingStateChanged(State state);

		void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths);
		void pathsCleared();

		void activePathUpdated(TrackingState trackingState);
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

	public static enum State {
		IDLE, UPDATING, CHOICE, TRACKING;

		private Set<State> allowedNextStates;

		static {
			IDLE.setAllowedNextStates(UPDATING, CHOICE);
			UPDATING.setAllowedNextStates(CHOICE);
			CHOICE.setAllowedNextStates(UPDATING, IDLE, TRACKING);
			TRACKING.setAllowedNextStates(IDLE, UPDATING, CHOICE);
		}

		private void setAllowedNextStates(State... nextStates) {
			allowedNextStates = new HashSet<State>(Arrays.asList(nextStates));
		}

		private boolean checkTransition(State state) {
			return allowedNextStates.contains(state);
		}
	}


	public static class TrackingState {
		public ChoicePath initialPath;
		public TrackPath<Point> trackingPath;
		public TrackPath.Result<Point> lastQueryResult;
	}
}
