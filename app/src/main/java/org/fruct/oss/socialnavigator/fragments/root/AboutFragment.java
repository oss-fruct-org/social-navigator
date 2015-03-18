package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;

public class AboutFragment extends Fragment {
	public AboutFragment() {
	}

	public static AboutFragment newInstance() {
		return new AboutFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		((MainActivity) activity).onSectionAttached(getString(R.string.title_section7),
				ActionBar.NAVIGATION_MODE_STANDARD, null);
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
}
