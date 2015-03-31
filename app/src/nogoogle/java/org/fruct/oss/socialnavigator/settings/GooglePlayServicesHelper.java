package org.fruct.oss.socialnavigator.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class GooglePlayServicesHelper {
	public static final int RC_SIGN_IN = 1;
	public static final int RC_GET_CODE = 2;

	public GooglePlayServicesHelper(Activity settingsActivity) {

	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {

	}

	public void setListener(Listener o) {
	}

	public void interrupt() {
	}

	public static boolean isAvailable(Context context) {
		return false;
	}

	public boolean check() {
		return false;
	}

	public void login() {

	}

	public static interface Listener {
		void onGoogleAuthFailed();
		void onGoogleAuthCompleted(String getsToken);
	}
}
