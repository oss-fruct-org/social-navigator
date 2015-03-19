package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.socialnavigator.App;
import org.fruct.oss.socialnavigator.GetsLoginActivity;
import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.adapters.CategoriesAdapter;
import org.fruct.oss.socialnavigator.annotations.Blocking;
import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.parsers.UserInfo;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.GetsProvider;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.points.PointsServiceConnection;
import org.fruct.oss.socialnavigator.points.PointsServiceConnectionListener;
import org.fruct.oss.socialnavigator.settings.GooglePlayServicesHelper;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class GetsFragment extends Fragment implements View.OnClickListener, GooglePlayServicesHelper.Listener,
		MainActivity.ActivityResultListener, PointsServiceConnectionListener {
	private static final Logger log = LoggerFactory.getLogger(GetsFragment.class);

	private GooglePlayServicesHelper googlePlayServicesHelper;
	private static final int RC_GETS_FRAGMENT = 3;

	private View publishLayout;
	private TextView publishTextView;
	private RecyclerView publishList;

	private View userInfoLayout;
	private ImageView userInfoImageView;
	private TextView userInfoTextView;

	private Button webLoginButton;
	private Button googleLoginButton;
	private Button logoutButton;

	private PointsService pointsService;
	private PointsServiceConnection pointsServiceConnection = new PointsServiceConnection(this);

	private CategoriesAdapter adapter;

	private UserInfoTask userInfoTask;
	private LogoutTask logoutTask;

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

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_gets, container, false);

		publishList = (RecyclerView) view.findViewById(android.R.id.list);
		publishLayout = view.findViewById(R.id.publish_layout);
		publishTextView = (TextView) view.findViewById(R.id.publish_text);

		webLoginButton = (Button) view.findViewById(R.id.button_login_web_browser);
		googleLoginButton = (Button) view.findViewById(R.id.button_login_google);
		logoutButton = (Button) view.findViewById(R.id.button_logout);

		userInfoLayout = view.findViewById(R.id.layout_user_info);
		userInfoTextView = (TextView) view.findViewById(R.id.text_user_name);
		userInfoImageView = (ImageView) view.findViewById(android.R.id.icon);

		webLoginButton.setOnClickListener(this);
		googleLoginButton.setOnClickListener(this);
		logoutButton.setOnClickListener(this);

		publishList.setHasFixedSize(true);
		publishList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

		updateViewState();
		setupAdapter();

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		pointsServiceConnection.bindService(getActivity());

		setHasOptionsMenu(true);
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

		if (userInfoTask != null) {
			userInfoTask.cancel(true);
		}

		if (logoutTask != null) {
			logoutTask.cancel(true);
		}

		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_login_web_browser:
			Intent intent = new Intent(getActivity(), GetsLoginActivity.class);
			startActivityForResult(intent, RC_GETS_FRAGMENT);
			break;

		case R.id.button_login_google:
			startGoogleLogin();
			break;

		case R.id.button_logout:
			Preferences appPref = new Preferences(getActivity());
			String getsToken = appPref.getGetsToken();

			if (getsToken != null) {
				logoutTask = new LogoutTask();
				logoutTask.execute(getsToken);
			}

			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RC_GETS_FRAGMENT) {
			if (resultCode != Activity.RESULT_OK) {
				Toast.makeText(getActivity(), R.string.str_google_login_web_error, Toast.LENGTH_LONG).show();
			} else {
				onGoogleAuthCompleted(data.getStringExtra("auth_token"));
			}
		}
	}

	@Override
	public void onActivityResultRedirect(int requestCode, int resultCode, Intent data) {
		if ((requestCode == GooglePlayServicesHelper.RC_SIGN_IN
				|| requestCode == GooglePlayServicesHelper.RC_GET_CODE)
				&& googlePlayServicesHelper != null) {
			googlePlayServicesHelper.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.refresh, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		Preferences appPref = new Preferences(getActivity());

		menu.findItem(R.id.action_refresh).setVisible(appPref.getGetsToken() != null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			Preferences appPref = new Preferences(getActivity());
			String getsToken = appPref.getGetsToken();

			if (getsToken != null) {
				userInfoTask = new UserInfoTask();
				userInfoTask.execute(getsToken);
			}
			return true;
		} else {
			return false;
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
			publishLayout.setVisibility(View.VISIBLE);
		} else {
			webLoginButton.setVisibility(View.VISIBLE);
			googleLoginButton.setVisibility(GooglePlayServicesHelper.isAvailable(getActivity())
					? View.VISIBLE : View.GONE);
			logoutButton.setVisibility(View.GONE);
			publishLayout.setVisibility(View.GONE);
		}

		UserInfo userInfo = appPref.getUserInfo();
		if (userInfo != null) {
			ImageLoader imageLoader = App.getImageLoader();
			imageLoader.displayImage(sizedImageUrl(userInfo.getImageUrl(),
					Utils.getDP(64)), userInfoImageView);
			userInfoTextView.setText(userInfo.getName());
			userInfoLayout.setVisibility(View.VISIBLE);
		} else {
			userInfoLayout.setVisibility(View.GONE);
		}

		if (userInfo != null && userInfo.isTrustedUser()) {
			publishList.setVisibility(View.VISIBLE);
			publishTextView.setVisibility(View.GONE);
		} else {
			publishList.setVisibility(View.GONE);
			publishTextView.setVisibility(View.VISIBLE);
		}
	}

	private void setupAdapter() {
		if (pointsService == null) {
			return;
		}

		List<Category> categories = pointsService.queryList(pointsService.requestCategories());
		adapter = new CategoriesAdapter(categories.toArray(new Category[categories.size()]));
		adapter.setPointsService(pointsService);
		publishList.setAdapter(adapter);
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
		getActivity().supportInvalidateOptionsMenu();

		userInfoTask = new UserInfoTask();
		userInfoTask.execute(getsToken);
	}

	private class UserInfoTask extends AsyncTask<String, Void, UserInfo> {
		@Override
		protected UserInfo doInBackground(String... params) {
			String getsToken = params[0];
			return updateUserInfo(getsToken);
		}

		@Override
		protected void onPostExecute(UserInfo userInfo) {
			if (userInfo == null) {
				return;
			}

			Preferences pref = new Preferences(getActivity());
			pref.setUserInfo(userInfo);

			updateViewState();
		}
	}

	private class LogoutTask extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			String getsToken = params[0];
			return logout(getsToken);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			Preferences appPref = new Preferences(getActivity());
			appPref.setGetsToken(null);
			appPref.setUserInfo(null);

			updateViewState();
			getActivity().supportInvalidateOptionsMenu();
		}
	}

	@Blocking
	private UserInfo updateUserInfo(String getsToken) {
		String request = createAuthTokenRequest(getsToken);
		try {
			String responseXml = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/userInfo.php", request);
			GetsResponse response = GetsResponse.parse(responseXml, UserInfo.class);

			return ((UserInfo) response.getContent());
		} catch (IOException | GetsException e) {
			log.error("Can't update user info", e);
			return null;
		}
	}

	@Blocking
	private boolean logout(String getsToken) {
		String request = createAuthTokenRequest(getsToken);
		try {
			String responseXml = Utils.downloadUrl(GetsProvider.GETS_SERVER + "/auth/revokeToken.php", request);
			GetsResponse response = GetsResponse.parse(responseXml, null);

			return response.getCode() == 0;
		} catch (IOException | GetsException e) {
			log.error("Can't update user info", e);
			return false;
		}
	}

	private String createAuthTokenRequest(String authToken) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.endDocument();

			return writer.toString();
		} catch (IOException ignore) {
			throw new RuntimeException("StringWriter thrown IOException");
		}
	}

	private String sizedImageUrl(String url, int newSize) {
		// https://lh0.googleusercontent.com/-BBBBBBBBBBBB/AAAAAAAAAAI/AAAAAAAAAYY/CCCCCCC/photo.jpg?sz=50
		int idx = url.lastIndexOf("?sz=");
		if (idx == -1)
			return url + "?sz=" + newSize;
		else
			return url.substring(0, idx) + "?sz=" + newSize;
	}
}
