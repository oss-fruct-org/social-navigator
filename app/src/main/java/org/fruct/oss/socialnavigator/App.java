package org.fruct.oss.socialnavigator;

import android.app.Application;
import android.content.Context;

import org.slf4j.impl.StaticLoggerBinder;

public class App extends Application {
	private static Context context;
	private static App app;

	@Override
	public void onCreate() {
		super.onCreate();
		StaticLoggerBinder.getSingleton();

		context = getApplicationContext();
		app = this;
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
