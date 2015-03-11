package org.fruct.oss.socialnavigator.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnection;
import org.fruct.oss.mapcontent.content.connections.ContentServiceConnectionListener;
import org.fruct.oss.socialnavigator.GetsLoginActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.utils.Utils;

public class SettingsActivity extends PreferenceActivity implements ContentServiceConnectionListener, GooglePlayServicesHelper.Listener {
	private static final int REQUEST_CODE = 2;

	private ListPreference storagePathPref;
	private SharedPreferences pref;

	private ContentServiceConnection contentServiceConnection = new ContentServiceConnection(this);
	private ContentService contentService;
	private Preference getsPref;
	private GooglePlayServicesHelper googlePlayServicesHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferenecs);

		pref = PreferenceManager.getDefaultSharedPreferences(this);

		storagePathPref = (ListPreference) findPreference(org.fruct.oss.mapcontent.content.Settings.PREF_STORAGE_PATH);
		getsPref = (Preference) findPreference(Preferences.PREF_GETS_TOKEN);

		setupGetsPreference();

		contentServiceConnection.bindService(this);
	}

	@Override
	protected void onDestroy() {
		contentServiceConnection.unbindService(this);

		if (googlePlayServicesHelper != null) {
			googlePlayServicesHelper.setListener(null);
			googlePlayServicesHelper.interrupt();
		}

		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		setupGetsPreference();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == GooglePlayServicesHelper.RC_SIGN_IN
				|| requestCode == GooglePlayServicesHelper.RC_GET_CODE)
				&& googlePlayServicesHelper != null) {
			googlePlayServicesHelper.onActivityResult(requestCode, resultCode, data);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void setupGetsPreference() {
		String token = pref.getString(Preferences.PREF_GETS_TOKEN, null);

		final boolean isLogIn = !Utils.isNullOrEmpty(token);

		if (isLogIn) {
			getsPref.setSummary("Click to logout");
		} else {
			getsPref.setSummary("Click to login");
		}

		getsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (isLogIn) {
					pref.edit().remove(Preferences.PREF_GETS_TOKEN).apply();
					setupGetsPreference();
				} else {
					startLogin();
				}
				return true;
			}
		});
	}

	private void startLogin() {
		if (!GooglePlayServicesHelper.isAvailable(this)) {
			Intent intent = new Intent(SettingsActivity.this, GetsLoginActivity.class);
			startActivity(intent);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			View view = getLayoutInflater().inflate(R.layout.dialog_login_method, null);

			Button browserButton = (Button) view.findViewById(R.id.button_login_web_browser);
			Button googleButton = (Button) view.findViewById(R.id.button_login_google);

			builder.setView(view);
			builder.setTitle(R.string.str_login_how);

			final AlertDialog dialog = builder.show();

			browserButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(SettingsActivity.this, GetsLoginActivity.class);
					startActivity(intent);
					dialog.dismiss();
				}
			});

			googleButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startGoogleLogin();
					dialog.dismiss();
				}
			});
		}
	}

	private void startGoogleLogin() {
		googlePlayServicesHelper = new GooglePlayServicesHelper(this);
		googlePlayServicesHelper.setListener(this);
		googlePlayServicesHelper.login();
	}

	@Override
	public void onGoogleAuthFailed() {
		Toast.makeText(this, R.string.str_google_login_error, Toast.LENGTH_LONG).show();
		googlePlayServicesHelper.setListener(null);
		googlePlayServicesHelper.interrupt();
	}

	@Override
	public void onGoogleAuthCompleted(String getsToken) {
		Toast.makeText(this, R.string.str_google_login_success, Toast.LENGTH_LONG).show();
		Preferences appPref = new Preferences(this);
		appPref.setGetsToken(getsToken);
		setupGetsPreference();
	}

	private void setupStoragePathPreference() {
		Utils.StorageDirDesc[] storagePaths = Utils.getPrivateStorageDirs(this);

		String[] names = new String[storagePaths.length];
		String[] paths = new String[storagePaths.length];

		String currentValue = pref.getString(org.fruct.oss.mapcontent.content.Settings.PREF_STORAGE_PATH, null);
		int currentNameRes = -1;

		for (int i = 0; i < storagePaths.length; i++) {
			names[i] = getString(storagePaths[i].nameRes);
			paths[i] = storagePaths[i].path;

			if (paths[i].equals(currentValue)) {
				currentNameRes = storagePaths[i].nameRes;
			}
		}

		storagePathPref.setEntryValues(paths);
		storagePathPref.setEntries(names);

		if (currentValue != null && currentNameRes != -1)
			storagePathPref.setSummary(currentNameRes);
	}

	@Override
	public void onContentServiceReady(ContentService contentService) {
		setupStoragePathPreference();
	}

	@Override
	public void onContentServiceDisconnected() {

	}
/*
	private class MigrateListener implements DataService.MigrateListener {
		private ProgressDialog dialog;

		@Override
		public void migrateFile(String name, int n, int max) {
			if (dialog == null) {
				dialog = ProgressDialog.show(SettingsActivity.this, "Copying", "Copying...", false, false);
			}

			dialog.setMax(max);
			dialog.setProgress(n);
			dialog.setMessage("Copying " + name);
		}

		@Override
		public void migrateFinished() {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}

			setupStoragePathPreference();

			Toast.makeText(SettingsActivity.this, getString(R.string.str_local_content_moved), Toast.LENGTH_LONG).show();
		}

		@Override
		public void migrateError() {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}

			Toast.makeText(SettingsActivity.this, getString(R.string.str_local_content_move_error), Toast.LENGTH_LONG).show();
		}
	}
	*/
}

