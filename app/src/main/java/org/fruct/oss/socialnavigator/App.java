package org.fruct.oss.socialnavigator;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKSdk;

import org.fruct.oss.socialnavigator.settings.Preferences;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.File;

public class App extends MultiDexApplication {
	private static Context context;
	private static App app;
	private static ImageLoader imageLoader;

    VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker() {
        @Override
        public void onVKAccessTokenChanged(VKAccessToken oldToken, VKAccessToken newToken) {
            if (newToken == null) {
                Toast.makeText(app, "AccessToken invalidated", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(app, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    };

	@Override
	public void onCreate() {
		super.onCreate();
		StaticLoggerBinder.getSingleton();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String str = pref.getString(Preferences.PREF_GETS_TOKEN, null);

		context = getApplicationContext();
		app = this;

		File baseCacheDir = new File(Environment.getExternalStorageDirectory().getPath());
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
		vkAccessTokenTracker.startTracking();
		VKSdk.initialize(this);
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
