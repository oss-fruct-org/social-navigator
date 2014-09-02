package org.fruct.oss.socialnavigator;

import android.app.Application;

import org.slf4j.impl.StaticLoggerBinder;

public class App extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		StaticLoggerBinder.getSingleton();
	}
}
