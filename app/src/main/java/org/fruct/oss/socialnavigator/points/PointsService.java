package org.fruct.oss.socialnavigator.points;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.utils.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

	private PointsProvider pointsProvider = null;

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

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!isTestMode) {
					setupProviders();
				}
			}
		}, 1000);

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
		database.close();
		database = null;

		executor.execute(new Runnable() {
			@Override
			public void run() {
				pointsProvider.close();
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

	public void refresh() {
		if (refreshProvidersTask != null && !refreshProvidersTask.isDone())
			refreshProvidersTask.cancel(true);

		refreshProvidersTask = executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					refreshProvider();

					if (!Thread.currentThread().isInterrupted()) {
						notifyDataUpdated();
					}
				} catch (Exception ex) {
					// TODO: refreshProvider should throw specific checked exception
					log.error("Cannot refresh provider", ex);
					notifyDataUpdateFailed(ex);
				}
			}
		});
	}

	public void setPointsProvider(PointsProvider pointsProvider) {
		if (this.pointsProvider != null) {
			throw new IllegalArgumentException("Provider with name " + pointsProvider.getProviderName() + " already exists");
		}

		this.pointsProvider = pointsProvider;
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
				return new Category(cursor);
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
				return new Point(cursor);
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
		};
	}

	@Blocking
	private void refreshProvider() throws PointsException {
		if (pointsProvider == null) {
			throw new IllegalArgumentException("Trying to refresh non-existing provider");
		}

		try {
			List<Disability> disabilities = pointsProvider.loadDisabilities();
			if (disabilities != null) {
				database.setDisabilities(disabilities);
			}
		} catch (PointsException e) {
			notifyDataUpdateFailed(e);
			return;
		}

		List<Category> categories;
		try {
			categories = pointsProvider.loadCategories();
		} catch (PointsException e) {
			notifyDataUpdateFailed(e);
			return;
		}

		for (Category category : categories) {
			log.trace("Category received: {}", category.getName());
			database.insertCategory(category);

			List<Point> points;
			try {
				points = pointsProvider.loadPoints(category);
			} catch (PointsException ex) {
				continue;
			}

			for (Point point : points) {
				log.trace(" Point received: {}", point.getName());

				if (point.getCategoryId() != category.getId()) {
					log.error(" Point's category doesn't equals category it loaded from: '{}' != '{}'",
							point.getCategoryId(), category.getId());
					continue;
				}

				database.insertPoint(point);

				if (Thread.currentThread().isInterrupted()) {
					return;
				}
			}
		}
	}

	public void awaitBackgroundTasks() {
		final CountDownLatch latch = new CountDownLatch(1);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				latch.countDown();
			}
		});
		try {
			if (!latch.await(100, TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Too long wait");
			}
		} catch (InterruptedException ignore) {
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	private void setupProviders() {
		GetsProvider getsProvider;
		getsProvider = new GetsProvider();
		setPointsProvider(getsProvider);
		refresh();
	}

	public void addPoint(Point point) {
		database.insertPoint(point);
		notifyDataUpdated();
	}

	public void setDisabilityState(Disability disability, boolean isActive) {
		database.setDisabilityState(disability, isActive);
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
