package org.fruct.oss.socialnavigator.gets;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.fruct.oss.socialnavigator.BuildConfig;
import org.fruct.oss.socialnavigator.parsers.IContent;
import org.fruct.oss.socialnavigator.utils.Utils;

import java.io.IOException;

public abstract class QueryTask<T> extends AsyncTask<Void, Void, T> {
	public static final String GETS_SERVER;
	public static final String DISABILITIES_LIST;

	static {
		if (!BuildConfig.DEBUG) {
			GETS_SERVER = "http://gets.cs.petrsu.ru/obstacle/service";
			DISABILITIES_LIST = "http://gets.cs.petrsu.ru/obstacle/config/disabilities.xml";
		} else {
			GETS_SERVER = "http://gets.cs.petrsu.ru/obstacle/service";
			DISABILITIES_LIST = "http://gets.cs.petrsu.ru/obstacle/config/disabilities.xml";
		}
	}

	private Throwable throwable;

	public T query() {
		return doInBackground();
	}

	@Override
	protected T doInBackground(Void... params) {
		String requestUrl = getRequestUrl();
		String postQuery = getPostQuery();

		try {
			String response = Utils.downloadUrl(requestUrl, postQuery);
			return parseContent(response);
		} catch (Exception e) {
			throwable = e;
			return null;
		}
	}

	public Throwable getException() {
		return throwable;
	}

	@Nullable
	protected abstract String getPostQuery();

	@NonNull
	protected abstract String getRequestUrl();

	@Nullable
	protected abstract T parseContent(String response) throws ParseException;
}
