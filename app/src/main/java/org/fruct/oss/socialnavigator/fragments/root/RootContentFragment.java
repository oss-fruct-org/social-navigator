package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import org.fruct.oss.mapcontent.content.connections.GHContentServiceConnection;
import org.fruct.oss.mapcontent.content.fragments.ContentFragment;
import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;

public class RootContentFragment extends ContentFragment {
	public static RootContentFragment newInstance() {
		return newInstance(false, false);
	}

	public static RootContentFragment newInstance(boolean suggestItem, boolean switchToUpdate) {
		RootContentFragment fragment = new RootContentFragment();

		Bundle args = new Bundle();
		args.putBoolean("suggest", suggestItem);
		args.putBoolean("update", switchToUpdate);
		fragment.setArguments(args);

		return fragment;
	}

	public RootContentFragment() {
		super();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section3), ActionBar.NAVIGATION_MODE_LIST, null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		remoteContentServiceConnection = new GHContentServiceConnection(this);
	}
}
