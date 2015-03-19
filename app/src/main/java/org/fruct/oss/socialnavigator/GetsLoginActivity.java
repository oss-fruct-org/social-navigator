package org.fruct.oss.socialnavigator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.parsers.AuthRedirectResponse;
import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.parsers.Kml;
import org.fruct.oss.socialnavigator.parsers.TokenContent;
import org.fruct.oss.socialnavigator.points.GetsProvider;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.utils.Utils;

import java.io.IOException;


public class GetsLoginActivity extends ActionBarActivity {
	public static final int RESULT_FAILED = RESULT_FIRST_USER;

	private WebView webView;

	private AsyncTask<Void, Void, AuthRedirectResponse> stage1Task;
	private AsyncTask<String, Void, String> stage2Task;

	private String code;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gets_login);

		webView = (WebView) findViewById(R.id.web_view);

		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);

		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				if (url.startsWith(GetsProvider.GETS_SERVER + "/include/GoogleAuth.php")) {
					startStage2();
				}
			}
		});

		if (savedInstanceState != null) {
			code = savedInstanceState.getString("code");
			if (code != null) {
				webView.restoreState(savedInstanceState);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (stage1Task != null) {
			stage1Task.cancel(true);
			stage1Task = null;
		}

		if (stage2Task != null) {
			stage2Task.cancel(true);
			stage2Task = null;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (code == null) {
			startStage1();
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("code", code);

		if (code != null) {
			webView.saveState(outState);
		}
	}

	private void showPage(String redirectUrl) {
		webView.loadUrl(redirectUrl);
	}

	private void startStage1() {
		stage1Task = new AsyncTask<Void, Void, AuthRedirectResponse>() {
			@Override
			protected AuthRedirectResponse doInBackground(Void... params) {
				String request = "<request><params/></request>";
				try {
					String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/userLogin.php", request);
					GetsResponse getsResponse = GetsResponse.parse(response, AuthRedirectResponse.class);

					return ((AuthRedirectResponse) getsResponse.getContent());
				} catch (IOException e) {
					return null;
				} catch (GetsException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(AuthRedirectResponse authRedirectResponse) {
				super.onPostExecute(authRedirectResponse);

				if (authRedirectResponse == null) {
					setResult(RESULT_FAILED);
					finish();
					return;
				}

				code = authRedirectResponse.getSessionId();

				showPage(authRedirectResponse.getRedirectUrl());
			}
		};

		stage1Task.execute();
	}

	private void startStage2() {
		stage2Task = new AsyncTask<String, Void, String>() {
			@Override
			protected String doInBackground(String... params) {
				String request = "<request><params><id>" + params[0] + "</id></params></request>";

				try {
					String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/userLogin.php", request);
					GetsResponse getsResponse = GetsResponse.parse(response, TokenContent.class);

					if (getsResponse.getContent() == null) {
						return null;
					}

					return ((TokenContent) getsResponse.getContent()).getAccessToken();
				} catch (IOException e) {
					return null;
				} catch (GetsException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String token) {
				super.onPostExecute(token);

				if (token == null) {
					setResult(RESULT_FAILED);
					finish();
				} else {
					setResult(RESULT_OK, new Intent().putExtra("auth_token", token));
					finish();
				}
			}
		};
		stage2Task.execute(code);
	}
}
