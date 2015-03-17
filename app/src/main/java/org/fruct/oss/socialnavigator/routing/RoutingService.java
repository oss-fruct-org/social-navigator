package org.fruct.oss.socialnavigator.routing;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import com.graphhopper.util.PointList;

import org.fruct.oss.mapcontent.BuildConfig;
import org.fruct.oss.mapcontent.content.ContentItem;
import org.fruct.oss.mapcontent.content.ContentListenerAdapter;
import org.fruct.oss.mapcontent.content.ContentManagerImpl;
import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnection;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnectionListener;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.points.PointsServiceConnection;
import org.fruct.oss.socialnavigator.points.PointsServiceConnectionListener;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.utils.EarthSpace;
import org.fruct.oss.socialnavigator.utils.NamedThreadFactory;
import org.fruct.oss.socialnavigator.utils.Space;
import org.fruct.oss.socialnavigator.utils.Timer;
import org.fruct.oss.socialnavigator.utils.TrackPath;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class RoutingService extends Service implements PointsService.Listener,
		LocationReceiver.Listener,	ContentServiceConnectionListener, PointsServiceConnectionListener {
	public static final String ARG_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_LOCATION";
	public static final String BC_LOCATION = "org.fruct.oss.socialnavigator.routing.RoutingService.BC_LOCATION";
	public static final RoutingType[] REQUIRED_ROUTING_TYPES = {
			RoutingType.SAFE,
			RoutingType.NORMAL,
			RoutingType.FASTEST};
	public static final int TRACKING_FINISH_DISTANCE = 10;
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);
	private final Binder binder = new Binder();
	private final ExecutorService executor
			= Executors.newSingleThreadExecutor(new NamedThreadFactory("RoutingServiceThread"));

	// Locks current target point at current paths
	private final Object mutex = new Object();

	// Locks services assignment
	private final Object serviceMutex = new Object();

	// Tasks
	private Future<?> routeFuture;
	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	private Routing routing;
	private PointsServiceConnection pointsServiceConnection = new PointsServiceConnection(this);
	private PointsService pointsService;

	private ContentServiceConnection contentServiceConnection = new ContentServiceConnection(this);
	private ContentService contentService;

	private LocationReceiver locationReceiver;

	private Handler handler;

	private ContentItem recommendedContentItem;

	private boolean isChoiceNeedsUpdate;
	private boolean isTrackingNeedsUpdate;
	private final Map<RoutingType, ChoicePath> currentPathsMap = Collections.synchronizedMap(new EnumMap<RoutingType, ChoicePath>(RoutingType.class));
	private TrackingState trackingState;

	// Variables that represent routing and must be persistent
	private GeoPoint sourceLocation;
	private GeoPoint destinationLocation;
	private RoutingType currentRoutingType;
	private State state;

	@Override
	public void onCreate() {
		super.onCreate();

		restoreCurrentState();

		if (state == null) {
			state = State.IDLE;
		}

		isChoiceNeedsUpdate = true;
		isTrackingNeedsUpdate = true;

		handler = new Handler(Looper.getMainLooper());

		locationReceiver = new LocationReceiver(this);
		locationReceiver.setListener(this);
		locationReceiver.start();

		routing = new Routing();

		currentPathsMap.clear();

		pointsServiceConnection.bindService(this);
		contentServiceConnection.bindService(this);

		log.info("created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler.removeCallbacks(stopRunnable);

		return RoutingService.START_NOT_STICKY;
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


		locationReceiver.stop();
		locationReceiver.setListener(null);

		contentServiceConnection.unbindService(this);
		pointsServiceConnection.unbindService(this);

		saveCurrentState();

		log.info("destroyed");

		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		handler.postDelayed(stopRunnable, 10000);
		saveCurrentState();
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		handler.removeCallbacks(stopRunnable);
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

	@Override
	public void onDataUpdated(boolean isRemoteUpdate) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				setObstaclesPoints();
			}
		});
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {
	}

	public void startTracking(final RoutingType routingType) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				currentRoutingType = routingType;
				boolean changed = changeRoutingState(State.TRACKING);
				ensureStateValid();
				if (changed) {
					notifyRoutingStateChanged(State.TRACKING);
				}
			}
		});
	}

	public void stopTracking() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				isChoiceNeedsUpdate = true;
				State newState = destinationLocation == null ? State.IDLE : State.CHOICE;
				boolean changed = changeRoutingState(newState);
				ensureStateValid();
				if (changed) {
					notifyRoutingStateChanged(newState);
				}
			}
		});
	}

	public void setTargetPoint(final GeoPoint targetPoint) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				destinationLocation = targetPoint;
				isChoiceNeedsUpdate = true;
				boolean changed = changeRoutingState(State.CHOICE);
				ensureStateValid();
				if (changed) {
					notifyRoutingStateChanged(State.CHOICE);
				}
			}
		});
	}

	public void clearTargetPoint() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				sourceLocation = null;
				destinationLocation = null;
				isChoiceNeedsUpdate = true;
				boolean changed = changeRoutingState(State.IDLE);
				ensureStateValid();
				if (changed) {
					notifyRoutingStateChanged(State.IDLE);
				}
			}
		});
	}

	public void sendLastResult() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				ensureStateValid();
				notifyRoutingStateChanged(state);
			}
		});
	}

	public void sendLastLocation() {
		locationReceiver.sendLastLocation();
	}

	public void forceLocation(GeoPoint currentLocation) {
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
	public void newLocation(final Location location) {
		Intent intent = new Intent(BC_LOCATION);
		intent.putExtra(ARG_LOCATION, location);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

		if (contentService != null) {
			contentService.setLocation(location);
		}

		executor.execute(new Runnable() {
			@Override
			public void run() {
				isChoiceNeedsUpdate = true;
				isTrackingNeedsUpdate = true;
				ensureStateValid();
			}
		});
	}

	public Location getLastLocation() {
		return locationReceiver.getOldLocation();
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	private void saveCurrentState() {
		Preferences appPref = new Preferences(this);
		appPref.setGeoPoint("source_location", sourceLocation);
		appPref.setGeoPoint("target_location", destinationLocation);
		appPref.setEnum("routing-service-state", state);
		appPref.setEnum("routing-service-routing-type", currentRoutingType);
	}

	private void restoreCurrentState() {
		Preferences appPref = new Preferences(this);

		sourceLocation = appPref.getGeoPoint("source_location");
		destinationLocation = appPref.getGeoPoint("target_location");
		state = appPref.getEnum("routing-service-state", State.class);
		currentRoutingType = appPref.getEnum("routing-service-routing-type", RoutingType.class);
	}

	private void ensureStateValid() {
		State oldState = state;

		switch (state) {
		case IDLE:
			trackingState = null;
			currentPathsMap.clear();
			isChoiceNeedsUpdate = true;
			isTrackingNeedsUpdate = true;
			break;

		case TRACKING:
			if (trackingState == null) {
				if (!initializeTrackingState()) {
					break;
				}
			}

			if (isTrackingNeedsUpdate) {
				updateActivePath(locationReceiver.getOldLocation());
			}

			break;

		case CHOICE:
			trackingState = null;
			isTrackingNeedsUpdate = true;

			if (isChoiceNeedsUpdate) {
				ensureChoiceStateValid();
			}
			break;
		}

		if (state != oldState) {
			ensureStateValid();
		}
	}

	private boolean initializeTrackingState() {
		if (!routing.isReady()) {
			return false;
		}

		if (sourceLocation == null || destinationLocation == null) {
			changeRoutingState(State.IDLE);
			return false;
		}

		ChoicePath activePath = currentPathsMap.get(currentRoutingType);
		if (activePath == null) {
			activePath = updateChoiceRoute(sourceLocation, destinationLocation, currentRoutingType);
		}

		if (activePath == null) {
			changeRoutingState(State.IDLE);
			return false;
		}

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
				sourceLocation.getLatitude(),
				sourceLocation.getLongitude());

		return true;
	}

	private void ensureChoiceStateValid() {
		if (!routing.isReady()) {
			return;
		}

		sourceLocation = new GeoPoint(locationReceiver.getOldLocation());

		if (destinationLocation == null) {
			changeRoutingState(State.IDLE);
			return;
		}

		notifyProgressStateChanged(true);

		for (final RoutingType routingType : REQUIRED_ROUTING_TYPES) {
			updateChoiceRoute(sourceLocation, destinationLocation, routingType);
		}

		notifyProgressStateChanged(false);

		isChoiceNeedsUpdate = false;

		notifyPathsUpdated(destinationLocation, currentPathsMap);
	}

	@Blocking
	private ChoicePath updateChoiceRoute(GeoPoint sourceLocation, GeoPoint targetLocation,
								   RoutingType routingType) {
		ChoicePath path = routing.route(sourceLocation, targetLocation, routingType);

		if (path != null) {
			currentPathsMap.put(routingType, path);
		} else {
			currentPathsMap.remove(routingType);
		}

		return path;
	}

	private void updateActivePath(Location location) {
		Timer timer = new Timer().start();
		trackingState.lastQueryResult = trackingState.trackingPath.query(
				location.getLatitude(), location.getLongitude());
		log.debug("Updating active path took {} seconds", timer.stop().getSeconds());
		if (trackingState.lastQueryResult.remainingDist < TRACKING_FINISH_DISTANCE) {
			destinationLocation = null;
			stopTracking();
		} else {
			isTrackingNeedsUpdate = false;
			notifyActivePathUpdated(trackingState);
		}
	}

	@Blocking
	private boolean initializeRouting(ContentItem contentItem) {
		try {
			String path = contentService.requestContentItem(contentItem);
			routing.loadFromPref(this, path);
			setObstaclesPoints();
		} catch (Exception ex) {
			log.error("Error initializing routing for contentItem {}", contentItem.getName(), ex);
			return false;
		}

		return true;
	}

	private void setObstaclesPoints() {
		List<Point> obstaclesPoints;
		synchronized (serviceMutex) {
			if (!routing.isReady() || pointsService == null) {
				return;
			}

			// Set obstacles
			obstaclesPoints = pointsService.queryList(pointsService.requestPoints());
			routing.setObstacles(obstaclesPoints);
		}

		isTrackingNeedsUpdate = true;
		isChoiceNeedsUpdate = true;
		ensureStateValid();
	}

	private boolean changeRoutingState(State state) {
		if (BuildConfig.DEBUG && !this.state.checkTransition(state)) {
			throw new IllegalStateException("Can't change state from " + this.state + " to " + state);
		}

		boolean ret = this.state != state;
		this.state = state;
		return ret;
	}

	@Override
	public void onPointsServiceReady(PointsService pointsService) {
		synchronized (serviceMutex) {
			this.pointsService = pointsService;
		}
		pointsService.addListener(this);
	}

	@Override
	public void onPointsServiceDisconnected() {
		synchronized (serviceMutex) {
			this.pointsService = null;
		}
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

	private void notifyProgressStateChanged(final boolean isActive) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.progressStateChanged(isActive);
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

	private final ContentService.Listener contentListener = new ContentListenerAdapter() {
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

	private final Runnable stopRunnable = new Runnable() {
		@Override
		public void run() {
			stopSelf();
		}
	};

	public static enum State {
		IDLE, CHOICE, TRACKING;

		private Set<State> allowedNextStates;

		static {
			IDLE.setAllowedNextStates( CHOICE);
			CHOICE.setAllowedNextStates(IDLE, TRACKING);
			TRACKING.setAllowedNextStates(IDLE, CHOICE);
		}

		private void setAllowedNextStates(State... nextStates) {
			allowedNextStates = new HashSet<State>(Arrays.asList(nextStates));
		}

		private boolean checkTransition(State state) {
			return allowedNextStates.contains(state);
		}
	}

	public static interface Listener {
		void routingStateChanged(State state);
		void progressStateChanged(boolean isActive);

		void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths);
		void activePathUpdated(TrackingState trackingState);
	}

	public static class TrackingState {
		public ChoicePath initialPath;
		public TrackPath<Point> trackingPath;
		public TrackPath.Result<Point> lastQueryResult;
	}

	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}
}
