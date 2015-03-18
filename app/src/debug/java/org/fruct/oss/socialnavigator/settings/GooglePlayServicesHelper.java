package org.fruct.oss.socialnavigator.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;

import org.fruct.oss.mapcontent.BuildConfig;
import org.fruct.oss.socialnavigator.parsers.AuthParameters;
import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.parsers.TokenContent;
import org.fruct.oss.socialnavigator.points.GetsProvider;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class GooglePlayServicesHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	private static final Logger log = LoggerFactory.getLogger(GooglePlayServicesHelper.class);

	public static final int RC_SIGN_IN = 1;
	public static final int RC_GET_CODE = 2;

	private final Activity activity;
	private Listener listener;

	private GoogleApiClient client;

	private boolean intentInProgress;

	private AsyncTask<Void, Void, AuthParameters> stage1Task;
	private AsyncTask<Void, Void, String> stage2Task;

	private String scope;
	private String clientId;


	public GooglePlayServicesHelper(Activity activity) {
		this.activity = activity;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public static boolean isAvailable(Context context) {
		return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
	}

	@Override
	public void onConnected(Bundle bundle) {
		startStage1();
	}

	@Override
	public void onConnectionSuspended(int i) {
		client.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (!intentInProgress && connectionResult.hasResolution()) {
			try {
				intentInProgress = true;
				activity.startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
						RC_SIGN_IN, null, 0, 0, 0);
			} catch (IntentSender.SendIntentException e) {
				// The intent was canceled before it was sent.  Return to the default
				// state and attempt to connect to get an updated ConnectionResult.
				intentInProgress = false;
				client.connect();
			}
		} else {
			if (listener != null) {
				listener.onGoogleAuthFailed();
			}
		}
	}

	public void onActivityResult(int requestCode, int responseCode, Intent intent) {
		if (requestCode == RC_SIGN_IN) {
			intentInProgress = false;

			if (!client.isConnecting() && responseCode == Activity.RESULT_OK) {
				client.connect();
			}
		} else if (requestCode == RC_GET_CODE) {
			if (responseCode == Activity.RESULT_OK) {
				startStage2();
			}
		}
	}

	public void login() {
		client = new GoogleApiClient.Builder(activity)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_LOGIN)
				.build();
		client.connect();
	}

	public void interrupt() {
		if (stage1Task != null) {
			stage1Task.cancel(true);
		}

		if (stage2Task != null) {
			stage2Task.cancel(true);
		}

		if (client.isConnected()) {
			client.disconnect();
		}
	}

	private void startStage1() {
		stage1Task = new AsyncTask<Void, Void, AuthParameters>() {
			@Override
			protected AuthParameters doInBackground(Void... params) {
				String request = "<request><params/></request>";
				try {
					String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/auth/getAuthParameters.php", request);
					GetsResponse getsResponse = GetsResponse.parse(response, AuthParameters.class);

					return ((AuthParameters) getsResponse.getContent());
				} catch (IOException e) {
					return null;
				} catch (GetsException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(AuthParameters authParameters) {
				super.onPostExecute(authParameters);

				if (authParameters == null) {
					if (listener != null) {
						listener.onGoogleAuthFailed();
					}
					return;
				}

				scope = authParameters.getScope();
				clientId = authParameters.getClientId();
				startStage2();
			}
		};

		stage1Task.execute();
	}

	private void startStage2() {
		stage2Task = new AsyncTask<Void, Void, String>() {
			private boolean isInRecoveryMode;

			@Override
			protected String doInBackground(Void... params) {
				Bundle bundle = new Bundle();
				bundle.putString(GoogleAuthUtil.KEY_REQUEST_VISIBLE_ACTIVITIES,
						"http://schemas.google.com/AddActivity");

				String scopeFull = "oauth2:server:client_id:" + clientId + ":api_scope:" + scope;
				try {
					String exchangeToken = GoogleAuthUtil.getToken(
							activity,
							Plus.AccountApi.getAccountName(client),
							scopeFull
					);

					log.info("Exchange token received {}", exchangeToken);

					// Login in GeTS using exchange token
					String request = createExchangeRequest(exchangeToken);
					String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/auth/exchangeToken.php", request);
					GetsResponse getsResponse = GetsResponse.parse(response, TokenContent.class);

					return ((TokenContent) getsResponse.getContent()).getAccessToken();
				} catch (IOException ex) {
					log.error("Google client io exception:", ex);
				} catch (UserRecoverableAuthException ex) {
					isInRecoveryMode = true;
					activity.startActivityForResult(ex.getIntent(), RC_GET_CODE);
				} catch (GoogleAuthException ex) {
					log.error("Google client auth exception:", ex);
				} catch (Exception ex) {
					log.error("Google client unknown exception:", ex);
				}

				return null;
			}

			@Override
			protected void onPostExecute(String getsToken) {
				if (getsToken == null) {
					if (!isInRecoveryMode) {
						if (listener != null) {
							listener.onGoogleAuthFailed();
						}
					}

					return;
				}

				log.info("Gets token received {}", getsToken);
				if (listener != null)
					listener.onGoogleAuthCompleted(getsToken);
			}
		};

		stage2Task.execute();
	}

	private String createExchangeRequest(String exchangeToken) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			serializer.startTag(null, "exchange_token").text(exchangeToken).endTag(null, "exchange_token");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.endDocument();

			return writer.toString();
		} catch (IOException ignore) {
			throw new RuntimeException("StringWriter throw IOException");
		}
	}

	public static interface Listener {
		void onGoogleAuthFailed();
		void onGoogleAuthCompleted(String getsToken);
	}
}
