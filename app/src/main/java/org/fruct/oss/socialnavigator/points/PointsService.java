package org.fruct.oss.socialnavigator.points;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.utils.Function;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PointsService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
	private static final Logger log = LoggerFactory.getLogger(PointsService.class);

	public static final int POINT_UPDATE_INTERVAL = 60 * 3600;
	public static final int POINT_UPDATE_DISTANCE = 1000;

	public static final String PREF_LAST_UPDATE = "pref_last_update";

	private final Binder binder = new Binder();

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final List<Listener> listeners = new CopyOnWriteArrayList<>();

	private Handler handler;
	private SharedPreferences pref;

	private PointsDatabase database;

	// Tasks
	private Future<?> refreshProvidersTask;
	private Future<?> synchronizationTask;

	private Location lastLocation;

	public PointsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
		return binder;
    }

	@Override
	public void onCreate() {
		super.onCreate();

		database = new PointsDatabase(this);
		handler = new Handler(Looper.getMainLooper());

		pref = PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);

		synchronize();

		log.info("created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler.removeCallbacks(stopRunnable);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (refreshProvidersTask != null && !refreshProvidersTask.isDone())
			refreshProvidersTask.cancel(true);

		if (synchronizationTask != null && !synchronizationTask.isDone())
			synchronizationTask.cancel(true);

		pref.unregisterOnSharedPreferenceChangeListener(this);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				database.close();
			}
		});
		executor.shutdownNow();

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
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(Preferences.PREF_GETS_TOKEN)) {
			String token = sharedPreferences.getString(key, null);
			if (token != null) {
				synchronize();
				refresh();
			}
		}
	}

	public void commitRefreshTimeAndLocation(long timestamp, GeoPoint location) {
		Preferences appPref = new Preferences(this);

		appPref.setLastPointsUpdateTimestamp(timestamp);
		appPref.setGeoPoint(PREF_LAST_UPDATE, location);
	}

	public void refreshIfNeed() {
		if (lastLocation == null) {
			return;
		}

		Preferences appPref = new Preferences(this);

		long lastUpdateTime = appPref.getLastPointsUpdateTimestamp();
		long currentTime = System.currentTimeMillis();

		GeoPoint currentLocation = new GeoPoint(lastLocation);
		GeoPoint lastLocation = appPref.getGeoPoint(PREF_LAST_UPDATE);

		if (lastUpdateTime < 0 || currentTime - lastUpdateTime > POINT_UPDATE_INTERVAL
				|| lastLocation == null || currentLocation.distanceTo(lastLocation) > POINT_UPDATE_DISTANCE) {
			refresh();
		}
	}

	public void refresh() {
		if (refreshProvidersTask != null && !refreshProvidersTask.isDone())
			refreshProvidersTask.cancel(true);

		if (lastLocation == null) {
			return;
		}

		final GeoPoint geoPoint = new GeoPoint(lastLocation);

		refreshProvidersTask = executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					log.info("Starting points refresh for geoPoint {}", geoPoint);
					long refreshStartTime = System.nanoTime();
					refreshRemote(geoPoint);
					long refreshEndTime = System.nanoTime();
					log.info("Points refresh time: {}", (refreshEndTime - refreshStartTime) * 1e-9f);

					notifyDataUpdated(true);

					commitRefreshTimeAndLocation(System.currentTimeMillis(), geoPoint);
				} catch (Exception ex) {
					// TODO: refreshRemote should throw specific checked exception
					log.error("Cannot refresh provider", ex);
					notifyDataUpdateFailed(ex);
				}
			}
		});
	}

	public void synchronize() {
		if (synchronizationTask != null && !synchronizationTask.isDone())
			return;

		synchronizationTask = executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					doSynchronize();
					log.info("Points successfully synchronized");
				} catch (PointsException ex) {
					log.error("Points synchronization failed", ex);
					// TODO: report user
				}
			}
		});
	}

	public void setLocation(Location location) {
		this.lastLocation = location;

		refreshIfNeed();
	}

	@Blocking
	private void doSynchronize() throws PointsException {
		PointsProvider provider = setupProvider();
		Cursor pointsToUpload = database.loadNotSynchronizedPoints();

		while (pointsToUpload.moveToNext()) {
			Point point = new Point(pointsToUpload, 1);
			String newUuid = provider.uploadPoint(point);
			database.markAsUploaded(point, newUuid);
		}

		pointsToUpload.close();
	}

	private void notifyDataUpdated(final boolean isRemoteUpdate) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.onDataUpdated(isRemoteUpdate);
				}
			}
		});
	}

	private void notifyDataUpdateFailed(final Throwable throwable) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.onDataUpdateFailed(throwable);
				}
			}
		});
	}

	public void queryCursor(final Request<?> request, final Function<Cursor> callback) {
		new AsyncTask<Void, Void, Cursor>() {
			@Override
			protected Cursor doInBackground(Void... params) {
				return request.doQuery();
			}

			@Override
			protected void onPostExecute(Cursor cursor) {
				callback.call(cursor);
			}
		}.execute();
	}

	@Blocking
	public Cursor queryCursor(Request<?> request) {
		return request.doQuery();
	}

	@Blocking
	public <T> List<T> queryList(Request<T> request) {
		Cursor cursor = queryCursor(request);

		List<T> ret = new ArrayList<>();
		while (cursor.moveToNext()) {
			ret.add(request.cursorToObject(cursor));
		}
		return ret;
	}

	public Request<Category> requestCategories() {
		return new Request<Category>() {
			@Override
			public Cursor doQuery() {
				return database.loadCategories();
			}

			@Override
			public Category cursorToObject(Cursor cursor) {
				return new Category(cursor, 0);
			}

			@Override
			public int getId(Category category) {
				return category.getId();
			}
		};
	}

	public Request<Point> requestPoints() {
		return new Request<Point>() {
			@Override
			public Cursor doQuery() {
				return database.loadPoints();
			}

			@Override
			public Point cursorToObject(Cursor cursor) {
				return new Point(cursor, 1);
			}

			@Override
			public int getId(Point point) {
				throw new UnsupportedOperationException("Point doesn't has public id");
			}
		};
	}

	public Request<Point> requestPrivatePoints() {
		return new Request<Point>() {
			@Override
			public Cursor doQuery() {
				return database.loadPrivatePoints();
			}

			@Override
			public Point cursorToObject(Cursor cursor) {
				return new Point(cursor, 1);
			}

			@Override
			public int getId(Point point) {
				throw new UnsupportedOperationException("Point doesn't has public id");
			}
		};
	}

	public Request<Disability> requestDisabilities() {
		return new Request<Disability>() {
			@Override
			public Cursor doQuery() {
				return database.loadDisabilities();
			}

			@Override
			public Disability cursorToObject(Cursor cursor) {
				return new Disability(cursor);
			}

			@Override
			public int getId(Disability disability) {
				throw new UnsupportedOperationException("Point doesn't has public id");
			}
		};
	}

	@Blocking
	private void refreshRemote(GeoPoint geoPoint) throws PointsException {
		PointsProvider pointsProvider = setupProvider();
		
		List<Disability> disabilities = pointsProvider.loadDisabilities();
		if (disabilities != null) {
			database.setDisabilities(disabilities);
		}

		List<Category> categories;
		categories = pointsProvider.loadCategories();

		for (Category category : categories) {
			database.insertCategory(category);

			if (Thread.currentThread().isInterrupted()) {
				return;
			}

			List<Point> points;
			try {
				log.debug("Loading points for category {}", category.getName());
				points = pointsProvider.loadPoints(category, geoPoint);
				log.debug("Points loaded");
			} catch (PointsException ex) {
				continue;
			}

			log.debug("Inserting points to database");
			database.insertPoints(points);
			log.debug("Points inserted to database");
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	private PointsProvider setupProvider() {
		Preferences appPref = new Preferences(this);

		GetsProvider getsProvider;
		getsProvider = new GetsProvider(appPref.getGetsToken());
		return getsProvider;
	}

	public void addPoint(Point point) {
		database.insertPoint(point);
		notifyDataUpdated(false);
		synchronize();
	}

	public void addCategory(Category category) {
		database.insertCategory(category);
	}

	public void setDisabilityState(Disability disability, boolean isActive) {
		database.setDisabilityState(disability, isActive);
	}

	public void commitDisabilityStates() {
		notifyDataUpdated(false);
	}

	public class Binder extends android.os.Binder {
		public PointsService getService() {
			return PointsService.this;
		}
	}

	public interface Listener {
		void onDataUpdated(boolean isRemoteUpdate);
		void onDataUpdateFailed(Throwable throwable);
	}
}
