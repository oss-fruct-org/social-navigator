package org.fruct.oss.socialnavigator;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.util.VKUtil;

import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.connections.GHContentServiceConnection;
import org.fruct.oss.mapcontent.content.fragments.ContentFragment;
import org.fruct.oss.mapcontent.content.helper.ContentHelper;
import org.fruct.oss.socialnavigator.fragments.root.AboutFragment;
import org.fruct.oss.socialnavigator.fragments.root.DisabilitiesFragment;
import org.fruct.oss.socialnavigator.fragments.root.GetsFragment;
import org.fruct.oss.socialnavigator.fragments.root.HelpFragment;
import org.fruct.oss.socialnavigator.fragments.root.MapFragment;
import org.fruct.oss.socialnavigator.fragments.root.RootContentFragment;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.settings.SettingsActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.jraska.falcon.Falcon;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiPhoto;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKPhotoArray;
import com.vk.sdk.api.model.VKWallPostResult;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.audiblealert.AudioManager;
import org.fruct.oss.socialnavigator.fragments.overlays.ObstaclesOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.OverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.PositionOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.RouteOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.TrackingOverlayFragment;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.EarthSpace;
import org.fruct.oss.socialnavigator.utils.Space;
import org.fruct.oss.socialnavigator.utils.TrackPath;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.widget.FrameLayout.LayoutParams;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

	private static final String[] sMyScope = new String[]{
			VKScope.FRIENDS,
			VKScope.WALL,
			VKScope.PHOTOS,
			VKScope.NOHTTPS,
			VKScope.MESSAGES,
			VKScope.DOCS
	};

	private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1001;
	private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1002;
	private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1003;

	/**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    //??? private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    //??? private CharSequence mTitle;
	//??? private int mNavigationMode;
	//??? private ActivityResultListener mResultListener;

	private ContentHelper contentHelper;

	private boolean isFromSavedState;

	// картинка для отправки в соц.сети
	private Bitmap mBitMap = null;
	private String mMessage = "Social navigator project";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// запуск менюшки
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		// запуск стека окон
		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		// проверка прав (Андрюша 6+)
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                } else {
                    startService(new Intent(this, RoutingService.class));
                }
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (this.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                startService(new Intent(this, ContentService.class));
                startService(new Intent(this, PointsService.class));
            }
        } else {
            startService(new Intent(this, RoutingService.class));
            startService(new Intent(this, ContentService.class));
            startService(new Intent(this, PointsService.class));
        }


		// старт сервисов

		isFromSavedState = savedInstanceState != null;

		contentHelper = new ContentHelper(this, new GHContentServiceConnection(null));
        contentHelper.setRootURLs(Preferences.ROOT_URLS);
		contentHelper.enableNetworkNotifications();
		contentHelper.enableLocationProviderNotifications();
		contentHelper.enableUpdateNotifications(PendingIntent.getActivity(this, 0,
				new Intent(ContentFragment.ACTION_UPDATE_READY, null, this, MainActivity.class),
				PendingIntent.FLAG_ONE_SHOT));

		contentHelper.enableContentNotifications(PendingIntent.getActivity(this, 1,
				new Intent(ContentFragment.ACTION_SHOW_ONLINE_CONTENT, null, this, MainActivity.class),
				PendingIntent.FLAG_ONE_SHOT));
		openMapFragment();

		// соц.сети
		VKSdk.wakeUpSession(this, new VKCallback<VKSdk.LoginState>() {
			@Override
			public void onResult(VKSdk.LoginState res) {
				switch (res) {
					case LoggedOut:
						//showLogin();
						break;
					case LoggedIn:
						//showLogout();
						break;
					case Pending:
						break;
					case Unknown:
						break;
				}
			}

			@Override
			public void onError(VKError error) {

			}
		});

		String[] fingerprint = VKUtil.getCertificateFingerprint(this, this.getPackageName());
		log.debug("Fingerprint: " +fingerprint[0]);
	}

	@Override
	protected void onStart() {
		super.onStart();
		contentHelper.onStart(isFromSavedState);
	}

	@Override
	protected void onStop() {
		contentHelper.onStop();
		super.onStop();
	}

	public void displayView(int viewId) {
		Fragment fragment = null;
		String title = getString(R.string.app_name);

		if (viewId == R.id.nav_map) {
			fragment = MapFragment.newInstance();
			title = getString(R.string.title_section1);
		} else if (viewId == R.id.nav_disabilities) {
			fragment = DisabilitiesFragment.newInstance();
			title = getString(R.string.title_section2);
		} else if (viewId == R.id.nav_content) {
			fragment = RootContentFragment.newInstance();
			title = getString(R.string.title_section3);
		} else if (viewId == R.id.nav_gets) {
			fragment = GetsFragment.newInstance();
			title = getString(R.string.title_section4);
		} else if (viewId == R.id.nav_config) {
			startActivity(new Intent(this, SettingsActivity.class));
			title = getString(R.string.title_section5);
		} else if (viewId == R.id.nav_help) {
			fragment = HelpFragment.newInstance();
			title = getString(R.string.title_section6);
		} else if (viewId == R.id.nav_cardiacare) {
			this.runCardiaCare();
		} else if (viewId == R.id.nav_about) {
			fragment = AboutFragment.newInstance();
			title = getString(R.string.title_section7);
		} else {
			log.warn("Unknown fragment with id=" + viewId);
		}
		if (fragment != null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(R.id.container, fragment);
			ft.commit();
		}

		// set the toolbar title
		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(title);
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();
		displayView(id);
		return true;
	}

//	@Override
//	protected void onNewIntent(Intent intent) {
//		super.onNewIntent(intent);
//
//		String action = intent.getAction();
//		switch (action) {
//		case ContentFragment.ACTION_SHOW_ONLINE_CONTENT:
//			setRootFragment(RootContentFragment.newInstance(true, false));
//			break;
//
//		case ContentFragment.ACTION_UPDATE_READY:
//			setRootFragment(RootContentFragment.newInstance(false, true));
//			break;
//		}
//	}
//
//	@Override
//    public void onNavigationDrawerItemSelected(int position) {
//		Fragment fragment;
//
//		switch (position) {
//		case 0:
//			fragment = MapFragment.newInstance();
//			break;
//
//		case 1:
//			fragment = DisabilitiesFragment.newInstance();
//			break;
//
//		case 2:
//			fragment = RootContentFragment.newInstance();
//			break;
//
//		case 3:
//			fragment = GetsFragment.newInstance();
//			break;
//
//		case 4:
//			startActivity(new Intent(this, SettingsActivity.class));
//			return;
//
//		case 5:
//			fragment = HelpFragment.newInstance();
//			break;
//
//			case 6:
//			runCardiaCare();
//				return;
//
//		case 7:
//			fragment = AboutFragment.newInstance();
//			break;
//
//		default:
//			fragment = PlaceholderFragment.newInstance(position + 1);
//			break;
//		}
//
//		setRootFragment(fragment);
//	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

//	public void onSectionAttached(String title, int navigationMode, ActivityResultListener resultListener) {
//		mTitle = title;
//		mNavigationMode = navigationMode;
//		mResultListener = resultListener;
//    }

//	public void setRootFragment(Fragment fragment) {
//		mNavigationMode = ActionBar.NAVIGATION_MODE_STANDARD;
//		FragmentManager fragmentManager = getSupportFragmentManager();
//
//		fragmentManager.popBackStack(TAG_PANEL_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//
//		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//		fragmentTransaction.addToBackStack(TAG_PANEL_FRAGMENT);
//		fragmentTransaction.replace(R.id.container, fragment, "content_fragment");
//		fragmentTransaction.commit();
//	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

//		if (mResultListener != null) {
//			mResultListener.onActivityResultRedirect(requestCode, resultCode, data);
//		}
		if (mBitMap != null) {
			loadPhotoToMyVKWall(mBitMap, mMessage);
			this.openMapFragment();
		}
	}
//
//	public void restoreActionBar() {
//        ActionBar actionBar = getSupportActionBar();
//        actionBar.setNavigationMode(mNavigationMode);
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setTitle(mTitle);
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!mNavigationDrawerFragment.isDrawerOpen()) {
//            // Only show items in the action bar relevant to this screen
//            // if the drawer is not showing. Otherwise, let the drawer
//            // decide what to show in the action bar.
//            //getMenuInflater().inflate(R.menu.main, menu);
//            restoreActionBar();
//            return true;
//        }
//
//        return super.onCreateOptionsMenu(menu);
//    }
//
	public void openCategoriesFragment() {
		displayView(R.id.nav_disabilities);
	}

	public void openMapFragment() {
		displayView(R.id.nav_map);
	}

	public void runCardiaCare() {
		Context context = this.getApplicationContext();
		PackageManager manager = context.getPackageManager();
		Intent i = manager.getLaunchIntentForPackage("ru.cardiacare.cardiacare");
		if (i == null) {
			log.debug("Cardiacare not found");
			Intent goToMarket = new Intent(Intent.ACTION_VIEW)
					.setData(Uri.parse("market://details?id=ru.cardiacare.cardiacare"));
			if (goToMarket != null) {
				goToMarket.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(goToMarket);
			}
			return;
		}
		i.addCategory(Intent.CATEGORY_LAUNCHER);
		context.startActivity(i);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION:
			case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.

					startService(new Intent(this, RoutingService.class));

				} else {

					// TODO: permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}
			case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					// contacts-related task you need to do.

					startService(new Intent(this, ContentService.class));
					startService(new Intent(this, PointsService.class));
				} else {

					// TODO: permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	public void sendImage() {
		File screenshotFile = takeScreenshot();

		mBitMap = BitmapFactory.decodeFile(screenshotFile.getAbsolutePath());
		loadPhotoToMyVKWall(mBitMap, mMessage);
	}

	void loadPhotoToMyVKWall(final Bitmap photo, final String message) {
		if (!VKSdk.isLoggedIn()) {
			showLogin();
			return;
		}
		VKRequest request = VKApi.uploadWallPhotoRequest(new VKUploadImage(photo,
				VKImageParameters.jpgImage(0.9f)), getMyId(), 0);
		request.executeWithListener(new VKRequest.VKRequestListener() {
			@Override
			public void onComplete(VKResponse response) {
				// recycle bitmap
				VKApiPhoto photoModel = ((VKPhotoArray) response.parsedModel).get(0);
				makeVKPost(new VKAttachments(photoModel), message, getMyId());
				mBitMap = null;
			}
			@Override
			public void onError(VKError error) {
				// error
				log.debug("VK error: " + error.toString());
			}
		});
	}

	private void showLogin() {
		this.getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.container, new LoginFragment())
				.commit();
	}

	/*
 *************************************
 *  VK Post Message with Screenshot
 *************************************
 */
	void makeVKPost(VKAttachments att, String msg, final int ownerId) {
		VKParameters parameters = new VKParameters();
		parameters.put(VKApiConst.OWNER_ID, String.valueOf(ownerId));
		parameters.put(VKApiConst.ATTACHMENTS, att);
		parameters.put(VKApiConst.MESSAGE, msg);
		VKRequest post = VKApi.wall().post(parameters);
		post.setModelClass(VKWallPostResult.class);
		post.executeWithListener(new VKRequest.VKRequestListener() {
			@Override
			public void onComplete(VKResponse response) {
				// post was added
			}
			@Override
			public void onError(VKError error) {
				// error
			}
		});
	}


	int getMyId() {
		final VKAccessToken vkAccessToken = VKAccessToken.currentToken();
		return vkAccessToken != null ? Integer.parseInt(vkAccessToken.userId) : 0;
	}

	public static class LoginFragment extends android.support.v4.app.Fragment {
		private CallbackManager callbackManager;
		private LoginButton loginButton;

		public LoginFragment() {
			super();
		}



		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.fragment_login, container, false);

			v.findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					VKSdk.login(getActivity(), sMyScope);
				}
			});


			return v;
		}

	}


    /*
     *************************************
     *       VK End
     *************************************
     */

    /*
     *************************************
     *       Make Screenshot
     *************************************
     */

	public File takeScreenshot() {

		File screenshotFile = getScreenshotFile();

		Falcon.takeScreenshot(this, screenshotFile);

		String message = "Screenshot captured to " + screenshotFile.getAbsolutePath();
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

		return screenshotFile;
	}

	protected File getScreenshotFile() {
		File screenshotDirectory;
		try {
			screenshotDirectory = getScreenshotsDirectory(getApplicationContext());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS", Locale.getDefault());

		String screenshotName = dateFormat.format(new Date()) + ".png";
		return new File(screenshotDirectory, screenshotName);
	}

	private static File getScreenshotsDirectory(Context context) throws IllegalAccessException {
		String dirName = "screenshots_" + context.getPackageName();

		File rootDir;

		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			rootDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		} else {
			rootDir = context.getDir("screens", Context.MODE_PRIVATE);
		}

		File directory = new File(rootDir, dirName);

		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new IllegalAccessException("Unable to create screenshot directory " + directory.getAbsolutePath());
			}
		}

		return directory;
	}


    /*
     *************************************
     *       End Make Screenshot
     *************************************
     */


//	/**
//     * A placeholder fragment containing a simple view.
//     */
//    public static class PlaceholderFragment extends Fragment {
//        /**
//         * The fragment argument representing the section number for this
//         * fragment.
//         */
//        private static final String ARG_SECTION_NUMBER = "section_number";
//
//        /**
//         * Returns a new instance of this fragment for the given section
//         * number.
//         */
//        public static PlaceholderFragment newInstance(int sectionNumber) {
//            PlaceholderFragment fragment = new PlaceholderFragment();
//            Bundle args = new Bundle();
//            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//            fragment.setArguments(args);
//            return fragment;
//        }
//
//        public PlaceholderFragment() {
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//            return rootView;
//        }
//
//        @Override
//        public void onAttach(Activity activity) {
//            super.onAttach(activity);
//			((MainActivity) activity).onSectionAttached("Placeholder", ActionBar.NAVIGATION_MODE_STANDARD, null);
//		}
//    }
//
//	public static interface ActivityResultListener {
//		void onActivityResultRedirect(int requestCode, int resultCode, Intent data);
//	}
}
