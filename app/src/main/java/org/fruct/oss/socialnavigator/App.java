package org.fruct.oss.socialnavigator;

import android.app.Application;
import android.content.Context;

import org.slf4j.impl.StaticLoggerBinder;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;

public class App extends Application {
	private static Context context;
	private static App app;

	@Override
	public void onCreate() {
		super.onCreate();
		StaticLoggerBinder.getSingleton();

		context = getApplicationContext();
		app = this;

		try {
			//File httpCacheDir = new File(context.getCacheDir(), "http");
			File httpCacheDir = new File("/sdcard/testcache");
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			Class.forName("android.net.http.HttpResponseCache")
					.getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception ignore) {
		}
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
}
