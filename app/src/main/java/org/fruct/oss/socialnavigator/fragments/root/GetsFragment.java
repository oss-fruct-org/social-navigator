package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.GetsLoginActivity;
import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.adapters.CategoriesAdapter;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.points.PointsServiceConnection;
import org.fruct.oss.socialnavigator.points.PointsServiceConnectionListener;
import org.fruct.oss.socialnavigator.settings.GooglePlayServicesHelper;
import org.fruct.oss.socialnavigator.settings.Preferences;

import java.util.List;

public class GetsFragment extends Fragment implements View.OnClickListener, GooglePlayServicesHelper.Listener,
		MainActivity.ActivityResultListener, PointsServiceConnectionListener {
	private GooglePlayServicesHelper googlePlayServicesHelper;

	private Button webLoginButton;
	private Button googleLoginButton;
	private Button logoutButton;
	private RecyclerView listView;

	private PointsService pointsService;
	private PointsServiceConnection pointsServiceConnection = new PointsServiceConnection(this);

	private SharedPreferences pref;
	private CategoriesAdapter adapter;

	public static GetsFragment newInstance() {
		return new GetsFragment();
	}

	public GetsFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section4),
				ActionBar.NAVIGATION_MODE_STANDARD, this);

		pref = PreferenceManager.getDefaultSharedPreferences(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gets, container, false);

		webLoginButton = (Button) view.findViewById(R.id.button_login_web_browser);
		googleLoginButton = (Button) view.findViewById(R.id.button_login_google);
		logoutButton = (Button) view.findViewById(R.id.button_logout);
		listView = (RecyclerView) view.findViewById(android.R.id.list);

		webLoginButton.setOnClickListener(this);
		googleLoginButton.setOnClickListener(this);
		logoutButton.setOnClickListener(this);

		listView.setHasFixedSize(true);
		listView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

		updateViewState();
		setupAdapter();

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		pointsServiceConnection.bindService(getActivity());
	}

	@Override
	public void onStart() {
		super.onStart();
		updateViewState();
	}

	@Override
	public void onDestroy() {
		if (googlePlayServicesHelper != null) {
			googlePlayServicesHelper.setListener(null);
			googlePlayServicesHelper.interrupt();
		}

		pointsServiceConnection.unbindService(getActivity());

		if (adapter != null) {
			adapter.close();
		}

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_login_web_browser:
			Intent intent = new Intent(getActivity(), GetsLoginActivity.class);
			startActivity(intent);
			break;

		case R.id.button_login_google:
			startGoogleLogin();
			break;

		case R.id.button_logout:
			pref.edit().remove(Preferences.PREF_GETS_TOKEN).apply();
			updateViewState();
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == GooglePlayServicesHelper.RC_SIGN_IN
				|| requestCode == GooglePlayServicesHelper.RC_GET_CODE)
				&& googlePlayServicesHelper != null) {
			googlePlayServicesHelper.onActivityResult(requestCode, resultCode, data);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onPointsServiceReady(PointsService pointsService) {
		this.pointsService = pointsService;
		setupAdapter();
	}

	@Override
	public void onPointsServiceDisconnected() {
		this.pointsService = null;
		adapter.setPointsService(null);
	}

	private void updateViewState() {
		Preferences appPref = new Preferences(getActivity());
		boolean isLogged = appPref.getGetsToken() != null;

		if (isLogged) {
			webLoginButton.setVisibility(View.GONE);
			googleLoginButton.setVisibility(View.GONE);
			logoutButton.setVisibility(View.VISIBLE);
		} else {
			webLoginButton.setVisibility(View.VISIBLE);
			googleLoginButton.setVisibility(GooglePlayServicesHelper.isAvailable(getActivity())
					? View.VISIBLE : View.GONE);
			logoutButton.setVisibility(View.GONE);
		}
	}

	private void setupAdapter() {
		if (pointsService == null) {
			return;
		}

		List<Category> categories = pointsService.queryList(pointsService.requestCategories());
		adapter = new CategoriesAdapter(categories.toArray(new Category[categories.size()]));
		adapter.setPointsService(pointsService);
		listView.setAdapter(adapter);
	}

	private void startGoogleLogin() {
		googlePlayServicesHelper = new GooglePlayServicesHelper(getActivity());
		googlePlayServicesHelper.setListener(this);
		googlePlayServicesHelper.login();
	}

	@Override
	public void onGoogleAuthFailed() {
		Toast.makeText(getActivity(), R.string.str_google_login_error, Toast.LENGTH_LONG).show();
		googlePlayServicesHelper.setListener(null);
		googlePlayServicesHelper.interrupt();
	}

	@Override
	public void onGoogleAuthCompleted(String getsToken) {
		Toast.makeText(getActivity(), R.string.str_google_login_success, Toast.LENGTH_LONG).show();
		Preferences appPref = new Preferences(getActivity());
		appPref.setGetsToken(getsToken);

		updateViewState();
	}
}
