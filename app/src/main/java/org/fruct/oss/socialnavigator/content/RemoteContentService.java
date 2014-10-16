package org.fruct.oss.socialnavigator.content;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import org.fruct.oss.socialnavigator.DataService;
import org.fruct.oss.socialnavigator.content.contenttypes.GraphhopperMapType;
import org.fruct.oss.socialnavigator.content.contenttypes.MapsforgeMapType;
import org.fruct.oss.socialnavigator.utils.Region;
import org.fruct.oss.socialnavigator.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteContentService extends Service implements DataService.DataListener {
	public static final String[] REMOTE_CONTENT_URLS = {"http://oss.fruct.org/projects/roadsigns/root.xml"};

	public static final String GRAPHHOPPER_MAP = "graphhopper-map";
	public static final String MAPSFORGE_MAP = "mapsforge-map";

	private Binder binder = new Binder();

	private DataService dataService;
	private DataServiceConnection dataServiceConnection = new DataServiceConnection();

	private ExecutorService executor = Executors.newSingleThreadExecutor();

	// Data storage
	private NetworkStorage networkStorage;
	private WritableDirectoryStorage mainLocalStorage;
	private List<ContentStorage> localStorages = new ArrayList<ContentStorage>();
	private KeyValue digestCache;

	private Map<String, ContentType> contentTypes = new HashMap<String, ContentType>();

	private Map<String, Region> regions = new HashMap<String, Region>();

	public RemoteContentService() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		bindService(new Intent(this, DataService.class), dataServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		unbindService(dataServiceConnection);

		super.onDestroy();
	}



	@Override
	public void dataPathChanged(String newDataPath) {
		// Here we need to switch data path and notify data service

	}

	@Override
	public int getPriority() {
		return 1;
	}

	private class Binder extends android.os.Binder {
		RemoteContentService getService() {
			return RemoteContentService.this;
		}
	}

	private class DataServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Context context = RemoteContentService.this;

			dataService = ((DataService.Binder) service).getService();
			dataService.addDataListener(RemoteContentService.this);

			digestCache = new KeyValue(context, "digestCache");

			networkStorage = new NetworkStorage(REMOTE_CONTENT_URLS);
			mainLocalStorage = new WritableDirectoryStorage(digestCache, dataService.getDataPath() + "/storage");

			String[] additionalStoragePaths = Utils.getExternalDirs(context);
			for (String path : additionalStoragePaths) {
				DirectoryStorage storage = new DirectoryStorage(digestCache, path);
				localStorages.add(storage);
			}

			contentTypes.put(GRAPHHOPPER_MAP, new GraphhopperMapType(context, dataService, regions));
			contentTypes.put(MAPSFORGE_MAP, new MapsforgeMapType(context, regions));
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dataService = null;
		}
	}

	public interface ContentStateListener {
		void contentItemReady(ContentItem contentItem);
		void contentItemDeactivated();
	}
}
