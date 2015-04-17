package org.fruct.oss.socialnavigator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import org.fruct.oss.mapcontent.content.ContentService;
import org.fruct.oss.mapcontent.content.connections.GHContentServiceConnection;
import org.fruct.oss.mapcontent.content.fragments.ContentFragment;
import org.fruct.oss.mapcontent.content.helper.ContentHelper;
import org.fruct.oss.socialnavigator.fragments.root.AboutFragment;
import org.fruct.oss.socialnavigator.fragments.root.GetsFragment;
import org.fruct.oss.socialnavigator.fragments.root.HelpFragment;
import org.fruct.oss.socialnavigator.fragments.root.MapFragment;
import org.fruct.oss.socialnavigator.fragments.root.DisabilitiesFragment;
import org.fruct.oss.socialnavigator.fragments.root.RootContentFragment;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.settings.SettingsActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

	private static final String TAG_PANEL_FRAGMENT = "root_transaction";

	/**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
	private int mNavigationMode;
	private ActivityResultListener mResultListener;

	private ContentHelper contentHelper;

	private boolean isFromSavedState;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment)
				getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(
				R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		startService(new Intent(this, ContentService.class));
		startService(new Intent(this, PointsService.class));
		startService(new Intent(this, RoutingService.class));

		isFromSavedState = savedInstanceState != null;

		contentHelper = new ContentHelper(this, new GHContentServiceConnection(null));
		contentHelper.enableNetworkNotifications();
		contentHelper.enableLocationProviderNotifications();
		contentHelper.enableUpdateNotifications(PendingIntent.getActivity(this, 0,
				new Intent(ContentFragment.ACTION_UPDATE_READY, null, this, MainActivity.class),
				PendingIntent.FLAG_ONE_SHOT));

		contentHelper.enableContentNotifications(PendingIntent.getActivity(this, 1,
				new Intent(ContentFragment.ACTION_SHOW_ONLINE_CONTENT, null, this, MainActivity.class),
				PendingIntent.FLAG_ONE_SHOT));
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

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		String action = intent.getAction();
		switch (action) {
		case ContentFragment.ACTION_SHOW_ONLINE_CONTENT:
			setRootFragment(RootContentFragment.newInstance(true, false));
			break;

		case ContentFragment.ACTION_UPDATE_READY:
			setRootFragment(RootContentFragment.newInstance(false, true));
			break;
		}
	}

	@Override
    public void onNavigationDrawerItemSelected(int position) {
		Fragment fragment;

		switch (position) {
		case 0:
			fragment = MapFragment.newInstance();
			break;

		case 1:
			fragment = DisabilitiesFragment.newInstance();
			break;

		case 2:
			fragment = RootContentFragment.newInstance();
			break;

		case 3:
			fragment = GetsFragment.newInstance();
			break;

		case 4:
			startActivity(new Intent(this, SettingsActivity.class));
			return;

		case 5:
			fragment = HelpFragment.newInstance();
			break;

		case 6:
			fragment = AboutFragment.newInstance();
			break;

		default:
			fragment = PlaceholderFragment.newInstance(position + 1);
			break;
		}

		setRootFragment(fragment);
	}

	@Override
	public void onBackPressed() {
		FragmentManager fragmentManager = getSupportFragmentManager();

		int count = fragmentManager.getBackStackEntryCount();
		String name;
		do {
			FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(--count);
			fragmentManager.popBackStack();
			name = entry.getName();
		} while (name == null);

		if (name.equals(TAG_PANEL_FRAGMENT)) {
			finish();
		}
	}

	public void onSectionAttached(String title, int navigationMode, ActivityResultListener resultListener) {
		mTitle = title;
		mNavigationMode = navigationMode;
		mResultListener = resultListener;
    }

	public void setRootFragment(Fragment fragment) {
		mNavigationMode = ActionBar.NAVIGATION_MODE_STANDARD;
		FragmentManager fragmentManager = getSupportFragmentManager();

		fragmentManager.popBackStack(TAG_PANEL_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE);

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.addToBackStack(TAG_PANEL_FRAGMENT);
		fragmentTransaction.replace(R.id.container, fragment, "content_fragment");
		fragmentTransaction.commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (mResultListener != null) {
			mResultListener.onActivityResultRedirect(requestCode, resultCode, data);
		}
	}

	public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(mNavigationMode);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            //getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }

        return super.onCreateOptionsMenu(menu);
    }

	public void openCategoriesFragment() {
		mNavigationDrawerFragment.selectItem(1);
	}

	public void openMapFragment() {
		mNavigationDrawerFragment.selectItem(0);
	}

	/**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
			((MainActivity) activity).onSectionAttached("Placeholder", ActionBar.NAVIGATION_MODE_STANDARD, null);
		}
    }

	public static interface ActivityResultListener {
		void onActivityResultRedirect(int requestCode, int resultCode, Intent data);
	}
}
