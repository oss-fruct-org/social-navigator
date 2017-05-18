package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutFragment extends Fragment {
	private static final Logger log = LoggerFactory.getLogger(AboutFragment.class);
	public AboutFragment() {
	}

	public static AboutFragment newInstance() {
		return new AboutFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

//		((MainActivity) activity).onSectionAttached(getString(R.string.title_section7),
//				ActionBar.NAVIGATION_MODE_STANDARD, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_about, container, false);

		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			TextView versionView = (TextView) view.findViewById(R.id.textViewVersion);
			versionView.setText(versionView.getText() + " " + pInfo.versionName);
		} catch (PackageManager.NameNotFoundException ignore) {
		}

		return view;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		log.debug("BINGO");
		menu.clear();
	}
}
