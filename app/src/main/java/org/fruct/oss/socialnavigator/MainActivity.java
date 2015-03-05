package org.fruct.oss.socialnavigator;

import android.app.Activity;
import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Build;
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
import org.fruct.oss.socialnavigator.fragments.root.MapFragment;
import org.fruct.oss.socialnavigator.fragments.root.PointFragment;
import org.fruct.oss.socialnavigator.fragments.root.RootContentFragment;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.settings.SettingsActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

	public static final String ACTION_SWITCH = "org.fruct.oss.socialnavigator.MainActivity.ACTION_SWITCH";
	public static final String ARG_INDEX = "org.fruct.oss.socialnavigator.INDEX";
	public static final String ARG_ARGUMENTS = "org.fruct.oss.socialnavigator.ARGUMENTS";

	/**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
	private int mNavigationMode;

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

		startService(new Intent(this, PointsService.class));
		startService(new Intent(this, RoutingService.class));
		startService(new Intent(this, ContentService.class));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			HttpResponseCache.getInstalled().flush();
			log.info("HttpResponseCache network: {}", HttpResponseCache.getInstalled().getNetworkCount());
			log.info("HttpResponseCache hits: {}", HttpResponseCache.getInstalled().getHitCount());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if (intent.getAction().equals(ACTION_SWITCH)) {
			int index = intent.getIntExtra(ARG_INDEX, 0);
			Bundle arguments = intent.getBundleExtra(ARG_ARGUMENTS);
			mNavigationDrawerFragment.selectItem(index, arguments);
		}
	}

	@Override
    public void onNavigationDrawerItemSelected(int position, Bundle arguments) {
		Fragment fragment;

		switch (position) {
		case 0:
			fragment = MapFragment.newInstance();
			break;

		case 1:
			fragment = PointFragment.newInstance();
			break;

		case 2:
			fragment = RootContentFragment.newInstance();
			break;

		case 3:
			startActivity(new Intent(this, SettingsActivity.class));
			return;

		default:
			fragment = PlaceholderFragment.newInstance(position + 1);
			break;
		}

		if (arguments != null) {
			fragment.setArguments(arguments);
		}

        FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.popBackStack("root", FragmentManager.POP_BACK_STACK_INCLUSIVE);

		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.addToBackStack("root");
		fragmentTransaction.replace(R.id.container, fragment, "content_fragment");
		fragmentTransaction.commit();
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

		if (name.equals("root")) {
			finish();
		}
	}

	public void onSectionAttached(String title, int navigationMode) {
		mTitle = title;
		mNavigationMode = navigationMode;
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
		mNavigationDrawerFragment.selectItem(1, null);
	}

	public void openMapFragment() {
		mNavigationDrawerFragment.selectItem(0, null);
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
			((MainActivity) activity).onSectionAttached("Placeholder", ActionBar.NAVIGATION_MODE_STANDARD);
		}
    }

}
