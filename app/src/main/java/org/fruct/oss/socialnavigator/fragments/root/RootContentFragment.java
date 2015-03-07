package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import org.fruct.oss.mapcontent.content.fragments.ContentFragment;
import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;

public class RootContentFragment extends ContentFragment {
	public static ContentFragment newInstance() {
		return new RootContentFragment();
	}

	public static final String[] REMOTE_CONTENT_URLS = {
			"http://oss.fruct.org/projects/roadsigns/root.xml"};

	public RootContentFragment() {
		super();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section3), ActionBar.NAVIGATION_MODE_LIST);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRootUrls(new String[] { "http://kappa.cs.petrsu.ru/~ivashov/mordor.xml" });
	}
}
