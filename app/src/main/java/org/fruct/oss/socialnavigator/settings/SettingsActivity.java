package org.fruct.oss.socialnavigator.settings;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.widget.Toast;

import org.fruct.oss.mapcontent.content.DataService;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.utils.Utils;

public class SettingsActivity extends PreferenceActivity {
	private ListPreference storagePathPref;
	private SharedPreferences pref;

	private DataService.MigrateListener migrateListener = new MigrateListener();
	private DataServiceConnection dataServiceConnection = new DataServiceConnection();

	private DataService dataService;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferenecs);

		pref = PreferenceManager.getDefaultSharedPreferences(this);

		storagePathPref = (ListPreference) findPreference(org.fruct.oss.mapcontent.content.Settings.PREF_STORAGE_PATH);

		bindService(new Intent(this, DataService.class), dataServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		if (dataService != null) {
			dataService.setMigrateListener(null);
		}

		unbindService(dataServiceConnection);
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();

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

	private class DataServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			dataService = ((DataService.Binder) binder).getService();
			dataService.setMigrateListener(migrateListener);
			setupStoragePathPreference();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			dataService = null;
		}
	}
}
