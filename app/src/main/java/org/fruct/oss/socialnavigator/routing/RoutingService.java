package org.fruct.oss.socialnavigator.routing;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
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

public class RoutingService extends Service implements PointsService.Listener {
	private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

	public static final String ACTION_ROUTE = "org.fruct.oss.socialnavigator.routing.RoutingService.ACTION_ROUTE";

	public static final String ARG_TARGET = "org.fruct.oss.socialnavigator.routing.RoutingService.ARG_TARGET";

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private LocationManager locationManager;

	private final Object mutex = new Object();

	// Tasks
	private Future<Routing> initializeFuture;
	private Future<List<RouteResult>> routeFuture;

	private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Routing routing;

	private PointsServiceConnection pointsServiceConnection;
	private PointsService pointsService;

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

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null || intent.getAction() == null) {
			return RoutingService.START_NOT_STICKY;
		}

		if (intent.getAction().equals(ACTION_ROUTE)) {
			GeoPoint targetPoint = intent.getParcelableExtra(ARG_TARGET);
			newTargetPoint(targetPoint);
		}

		return RoutingService.START_NOT_STICKY;
	}

	public void sendLastResult() {
		if (routeFuture != null && routeFuture.isDone() && !routeFuture.isCancelled()) {
			try {
				notifyPathsUpdated(routeFuture.get());
			} catch (InterruptedException e) {
				log.error("routeFuturn thrown InterruptedException but has 'done' flag", e);
			} catch (ExecutionException e) {
				log.error("Can't get last route result due to exception", e);
			}
		}
	}

	private void newTargetPoint(final GeoPoint targetPoint) {
		if (routeFuture != null) {
			routeFuture.cancel(true);
		}

		final Location currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (currentLocation == null) {
			return;
		}

		routeFuture = executor.submit(new Callable<List<RouteResult>>() {
			@Override
			public List<RouteResult> call() {
				if (!ensureRoutingReady()) {
					// TODO: notify user that routing haven't been initialized
					return null;
				}

				List<RouteResult> results = routing.route(currentLocation.getLatitude(), currentLocation.getLongitude(),
						targetPoint.getLatitude(), targetPoint.getLongitude());

				notifyPathsUpdated(results);

				return results;
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

	private void notifyPathsUpdated(final List<RouteResult> results) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.pathsUpdated(results);
				}
			}
		});
	}

	public class Binder extends android.os.Binder {
		public RoutingService getService() {
			return RoutingService.this;
		}
	}

	public static class RouteResult {
		private PointList pointList;
		private String vehicle;
		private String weighting;

		RouteResult(PointList pointList, String vehicle, String weighting) {
			this.pointList = pointList;
			this.vehicle = vehicle;
			this.weighting = weighting;
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
	}

	public static interface Listener {
		void pathsUpdated(List<RouteResult> paths);
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
