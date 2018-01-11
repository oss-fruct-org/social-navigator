package org.fruct.oss.socialnavigator.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Xml;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import org.fruct.oss.mapcontent.BuildConfig;
import org.fruct.oss.socialnavigator.parsers.AuthParameters;
import org.fruct.oss.socialnavigator.parsers.AuthRedirectResponse;
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
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class GooglePlayServicesHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener { //, GoogleApiClient.ServerAuthCodeCallbacks {
	private static final Logger log = LoggerFactory.getLogger(GooglePlayServicesHelper.class);

	public static final int RC_SIGN_IN = 1111;
	public static final int RC_GET_CODE = 2222;
	public static final int RC_CHECK = 3333;

	private final Fragment activity;
	private Listener listener;

	private GoogleApiClient client;

	private boolean intentInProgress;

	private String scope;
	private String clientId;

	private AsyncTask<Void, Void, AuthParameters> stage1Task;
	private AsyncTask<Void, Void, GetsResponse> stage3Task;
	private Handler handler = new Handler(Looper.getMainLooper());

	public GooglePlayServicesHelper(Fragment activity) {
		this.activity = activity;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public static boolean isAvailable(Context context) {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
		return status == ConnectionResult.SUCCESS || GooglePlayServicesUtil.isUserRecoverableError(status);
	}

	public boolean check() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity.getActivity());
		if (status == ConnectionResult.SUCCESS) {
			return true;
		} else if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(status, activity.getActivity(), RC_CHECK);
			errorDialog.show();
			return false;
		} else {
			return false;
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
	}

	@Override
	public void onConnectionSuspended(int i) {
		client.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
	    log.warn("GooglePlayServicesHelper connectionError: " + connectionResult.getErrorCode() + ": " + connectionResult.getErrorMessage());
		if (!intentInProgress && connectionResult.hasResolution()) {
			try {
				intentInProgress = true;
				activity.getActivity().startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
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

	public void onActivityResult(int requestCode, int responseCode, Intent data) {
	    log.debug("GooglePlayServicesHelper responce=" + responseCode);
		if (requestCode == RC_SIGN_IN) {
			intentInProgress = false;

            GoogleSignInResult result =
                    Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount signInAccount = result.getSignInAccount();
                String serverAuthCode = signInAccount.getServerAuthCode();
                // Send serverAuthCode to server via HTTPS POST using Volley or AsyncTask.
                onUploadServerAuthCode(clientId, serverAuthCode);
            } else {
                log.warn("Error: " + GoogleSignInStatusCodes.getStatusCodeString(result.getStatus().getStatusCode()));
                Toast.makeText(activity.getContext(), "Something wrong: " + result.getStatus().getStatusCode() + ": " +
                        result.getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
            }

		}
	}

	public void login() {
		startStage1();
	}

	public void interrupt() {
		if (client != null && client.isConnected()) {
			client.disconnect();
		}

		if (stage1Task != null) {
			stage1Task.cancel(true);
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

					if (getsResponse.getCode() != 0) {
						return null;
					}

					return (AuthParameters) getsResponse.getContent();
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
					notifyAuthFailed();
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
		log.debug("Start auth stage 2: cleantid=" + clientId);
		GoogleSignInOptions gso =
				new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestEmail()
				.requestScopes(new Scope("https://www.googleapis.com/auth/plus.me"),
                        new Scope("https://www.googleapis.com/auth/plus.login"),
                        new Scope("https://www.googleapis.com/auth/drive"),
                        new Scope("https://www.googleapis.com/auth/userinfo.email"))
				.requestServerAuthCode(clientId, true)
				.build();

		client = new GoogleApiClient.Builder(activity.getActivity())
                .enableAutoManage(activity.getActivity(), this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();
		client.connect();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(client);
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);

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

//	@Override
//	public CheckResult onCheckServerAuthorization(String idToken, Set<Scope> set) {
//		Set<Scope> scopes = new HashSet<>();
//
//		StringTokenizer tokenizer = new StringTokenizer(scope, " ");
//
//		while (tokenizer.hasMoreElements()) {
//			scopes.add(new Scope(tokenizer.nextToken()));
//		}
//
//		return CheckResult.newAuthRequiredResult(scopes);
//	}

//	@Override
	void onUploadServerAuthCode(String idToken, String serverAuthCode) {
		final String request = createExchangeRequest(serverAuthCode);

		stage3Task = new AsyncTask<Void, Void, GetsResponse>() {
			@Override
			protected GetsResponse doInBackground(Void... params) {
				try {
                    String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/auth/exchangeToken.php", request);
                    GetsResponse getsResponse = GetsResponse.parse(response, TokenContent.class);

					return getsResponse;
				} catch (IOException e) {
					return null;
				} catch (GetsException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(GetsResponse getsResponse) {
				super.onPostExecute(getsResponse);

				if (getsResponse == null || getsResponse.getCode() != 0) {
                    Toast.makeText(activity.getContext(), "Can't finish auth process", Toast.LENGTH_LONG).show();
                    notifyAuthFailed();
                    return;
                }

                notifyAuthCompleted(((TokenContent) getsResponse.getContent()).getAccessToken());
            }
		};

		stage3Task.execute();

//		try {
//			String response = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/auth/exchangeToken.php", request);
//			GetsResponse getsResponse = GetsResponse.parse(response, TokenContent.class);
//
//			if (getsResponse.getCode() != 0) {
//				notifyAuthFailed();
//				return false;
//			}
//
//			notifyAuthCompleted(((TokenContent) getsResponse.getContent()).getAccessToken());
//
//			return true;
//		} catch (IOException e) {
//			notifyAuthFailed();
//			return false;
//		} catch (GetsException e) {
//			notifyAuthFailed();
//			return false;
//		}
	}

	private void notifyAuthFailed() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (listener != null) {
					listener.onGoogleAuthFailed();
				}
			}
		});
	}

	private void notifyAuthCompleted(final String getsToken) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (listener != null) {
					listener.onGoogleAuthCompleted(getsToken);
				}
			}
		});
	}

	public interface Listener {
		void onGoogleAuthFailed();
		void onGoogleAuthCompleted(String getsToken);
	}
}
