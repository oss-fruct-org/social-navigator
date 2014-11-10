package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.support.v7.app.ActionBar;

import org.fruct.oss.mapcontent.content.fragments.ContentFragment;
import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;

public class RootContentFragment extends ContentFragment {
	public static ContentFragment newInstance() {
		return new ContentFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section3), ActionBar.NAVIGATION_MODE_LIST);
	}
}
