package org.fruct.oss.socialnavigator;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.fruct.oss.socialnavigator.settings.Preferences;
import org.slf4j.impl.StaticLoggerBinder;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {
	private static Context context;
	private static App app;
	private static ImageLoader imageLoader;

	@Override
	public void onCreate() {
		super.onCreate();
		StaticLoggerBinder.getSingleton();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String str = pref.getString(Preferences.PREF_GETS_TOKEN, null);

		context = getApplicationContext();
		app = this;

		File baseCacheDir = new File("/sdcard/debug/sn-cache");
		if (!BuildConfig.DEBUG) {
			baseCacheDir = new File(context.getCacheDir(), "base");
		}

		File httpCacheDir = new File(baseCacheDir, "http");
		File imageCacheDir = new File(baseCacheDir, "image");

		imageLoader = setupImageLoader(imageCacheDir);

		/*try {
			if (!BuildConfig.DEBUG) {
				httpCacheDir = new File(context.getCacheDir(), "http");
			}

			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception ignore) {
		}*/
	}

	private ImageLoader setupImageLoader(File imageCacheDir) {
		ImageLoader imageLoader = ImageLoader.getInstance();

		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.build();

		ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this)
				.diskCacheSize(10 * 1024 * 1024)
				.diskCache(new UnlimitedDiscCache(imageCacheDir))
				.defaultDisplayImageOptions(defaultOptions)
				.imageDownloader(new BaseImageDownloader(this))
				.build();

		imageLoader.init(imageLoaderConfiguration);

		return imageLoader;
	}

	public static Context getContext() {
		if (context == null)
			throw new IllegalStateException("Application not initialized yet");

		return context;
	}

	public static App getInstance() {
		if (app == null)
			throw new IllegalStateException("Application not initialized yet");

		return App.app;
	}

	public static ImageLoader getImageLoader() {
		if (imageLoader == null)
			throw new IllegalStateException("Application not initialized yet");

		return App.imageLoader;
	}
}
