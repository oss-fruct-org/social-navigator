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
import org.fruct.oss.mapcontent.content.connections.GHContentServiceConnection;
import org.fruct.oss.socialnavigator.GetsLoginActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.utils.Utils;

public class SettingsActivity extends PreferenceActivity implements ContentServiceConnectionListener {
	private static final int REQUEST_CODE = 2;

	private ListPreference storagePathPref;
	private SharedPreferences pref;

	private ContentServiceConnection contentServiceConnection = new GHContentServiceConnection(this);
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

