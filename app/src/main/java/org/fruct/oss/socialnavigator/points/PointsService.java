package org.fruct.oss.socialnavigator.points;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.fruct.oss.socialnavigator.annotations.Blocking;
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

public class PointsService extends Service {
	private static final Logger log = LoggerFactory.getLogger(PointsService.class);

	private final Binder binder = new Binder();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final Map<String, PointsProvider> providerMap = new HashMap<String, PointsProvider>();
	private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

	private Handler handler;

	private PointsDatabase database;

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
	}

	@Override
	public void onDestroy() {
		database.close();
		database = null;

		executor.shutdown();

		super.onDestroy();
	}


	public void addPointsProvider(final PointsProvider pointsProvider) {
		if (providerMap.containsKey(pointsProvider.getProviderName())) {
			throw new IllegalArgumentException("Provider with name " + pointsProvider.getProviderName() + " already exists");
		}

		synchronized (providerMap) {
			providerMap.put(pointsProvider.getProviderName(), pointsProvider);
		}

		executor.execute(new Runnable() {
			@Override
			public void run() {
				refreshProvider(pointsProvider.getProviderName());
				notifyDataUpdated();
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

	@Blocking
	public Cursor queryCursor(Request<?> request) {
		return request.doQuery();
	}

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

	public Request<Point> requestPoints(final Category category) {
		return new Request<Point>() {
			@Override
			public Cursor doQuery() {
				return database.loadPoints(category);
			}

			@Override
			public Point cursorToObject(Cursor cursor) {
				return new Point(cursor);
			}
		};
	}

	public Request<Point> requestPoints(final int categoryId) {
		return new Request<Point>() {
			@Override
			public Cursor doQuery() {
				return database.loadPoints(categoryId);
			}

			@Override
			public Point cursorToObject(Cursor cursor) {
				return new Point(cursor);
			}
		};
	}

	@Blocking
	private void refreshProvider(String providerName) {
		PointsProvider provider = providerMap.get(providerName);
		if (provider == null) {
			throw new IllegalArgumentException("Trying to refresh non-existing provider");
		}

		List<Category> categories = provider.loadCategories();

		for (Category category : categories) {
			log.trace("Category received: {}", category.getName());
			List<Point> points = provider.loadPoints(category);
			database.insertCategory(category);

			for (Point point : points) {
				log.trace(" Point received: {}", point.getName());

				if (point.getCategoryId() != category.getId()) {
					log.error(" Point's category doesn't equals category it loaded from: '{}' != '{}'",
							point.getName(), category.getName());
					continue;
				}

				database.insertPoint(point);
			}
		}

	}

	public PointsService getService() {
		return this;
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
			latch.await();
		} catch (InterruptedException ignore) {
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public class Binder extends android.os.Binder {
		public PointsService getService() {
			return PointsService.this;
		}
	}

	public interface Listener {
		void onDataUpdated();
	}
}
