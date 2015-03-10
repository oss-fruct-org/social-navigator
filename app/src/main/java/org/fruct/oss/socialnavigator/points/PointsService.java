package org.fruct.oss.socialnavigator.points;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.fruct.oss.socialnavigator.BuildConfig;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.gets.DisabilitiesTask;
import org.fruct.oss.socialnavigator.utils.Function;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PointsService extends Service {
	private static final Logger log = LoggerFactory.getLogger(PointsService.class);

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Handler handler;

	private PointsDatabase database;

	// Tasks
	private Future<?> refreshProvidersTask;

	private boolean isTestMode;

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

		log.info("created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			isTestMode = intent.getBooleanExtra("test", false);
		}

		handler.removeCallbacks(stopRunnable);
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (refreshProvidersTask != null && !refreshProvidersTask.isDone())
			refreshProvidersTask.cancel(true);

		executor.execute(new Runnable() {
			@Override
			public void run() {
				database.close();
			}
		});

		executor.shutdown();

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

	public void refresh(final GeoPoint geoPoint) {
		if (refreshProvidersTask != null && !refreshProvidersTask.isDone())
			refreshProvidersTask.cancel(true);

		refreshProvidersTask = executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					log.info("Starting points refresh");
					long refreshStartTime = System.nanoTime();
					refreshRemote(geoPoint);
					long refreshEndTime = System.nanoTime();
					log.info("Points refresh time: {}", (refreshEndTime - refreshStartTime) * 1e-9f);

					notifyDataUpdated();
				} catch (Exception ex) {
					// TODO: refreshRemote should throw specific checked exception
					log.error("Cannot refresh provider", ex);
					notifyDataUpdateFailed(ex);
				}
			}
		});
	}

	private void notifyDataUpdated() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				for (Listener listener : listeners) {
					listener.onDataUpdated();
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

		List<T> ret = new ArrayList<T>();
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
		GetsProvider getsProvider;
		getsProvider = new GetsProvider();
		return getsProvider;
/*
		ArrayPointsProvider arrayPointsProvider = new ArrayPointsProvider("array-provider");
		arrayPointsProvider.setCategories("test-category");

		arrayPointsProvider.addPointDesc("Test point", "Test description", "http://example.com",
				"test-category", 61.786136, 34.355825, 7);
		arrayPointsProvider.addPointDesc("Test point 2", "Test description 2", "http://example.com",
				"test-category", 61.78623, 34.356029, 7);

		setPointsProvider(arrayPointsProvider);*/
	}

	public void addPoint(Point point) {
		database.insertPoint(point);
		notifyDataUpdated();
	}

	public void setDisabilityState(Disability disability, boolean isActive) {
		database.setDisabilityState(disability, isActive);
	}

	public void commitDisabilityStates() {
		notifyDataUpdated();
	}

	public class Binder extends android.os.Binder {
		public PointsService getService() {
			return PointsService.this;
		}
	}

	public interface Listener {
		void onDataUpdated();
		void onDataUpdateFailed(Throwable throwable);
	}
}
