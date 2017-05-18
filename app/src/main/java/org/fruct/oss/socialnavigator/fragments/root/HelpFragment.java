package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HelpFragment extends Fragment implements ActionBar.TabListener {
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link android.support.v4.view.ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	public HelpFragment() {
	}

	public static HelpFragment newInstance() {
		return new HelpFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_help, container, false);

		// Set up the action bar.
		final android.support.v7.app.ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		actionBar.removeAllTabs();

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(
					actionBar.newTab()
							.setText(mSectionsPagerAdapter.getPageTitle(i))
							.setTabListener(this));
		}

		actionBar.setDisplayHomeAsUpEnabled(true);

		return view;
	}

	@Override
	public void onTabSelected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(android.support.v7.app.ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	private HelpEntry newHelpEntry(@StringRes int titleString, @StringRes int contentString,
								   @DrawableRes int iconRes, boolean invert) {
		return new HelpEntry(getString(titleString), getString(contentString), iconRes, invert);
	}

	/**
	 * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			HelpEntry[] helpEntries1 = {
					newHelpEntry(R.string.help_1_place_title, R.string.help_1_place,
							R.drawable.ic_action_location_found, false),
					newHelpEntry(R.string.help_1_refresh_title, R.string.help_1_refresh,
							R.drawable.ic_action_refresh, false),
					newHelpEntry(R.string.help_1_click_title, R.string.help_1_click,
							0, false),

					newHelpEntry(R.string.help_1_route_title, R.string.help_1_route,
							0, false),
					newHelpEntry(R.string.help_1_create_point_title, R.string.help_1_create_point,
							0, false),
					newHelpEntry(R.string.help_1_menu_place_here_title, R.string.help_1_menu_place_here,
							0, false),

					newHelpEntry(R.string.help_1_choose_routing_type_title, R.string.help_1_choose_routing_type,
							R.drawable.ic_action_edit, false),
					newHelpEntry(R.string.help_1_accept_route_title, R.string.help_1_accept_route,
							R.drawable.ic_action_accept, false),

			};

			HelpEntry[] helpEntries2 = {
					newHelpEntry(R.string.help_2_refresh_title,R.string.help_2_refresh,
							R.drawable.ic_action_refresh, false),
					newHelpEntry(R.string.help_2_disabilities_title,R.string.help_2_disabilities,
							R.drawable.ic_action_accept, false)
			};

			HelpEntry[] helpEntries3 = {
					newHelpEntry(R.string.help_3_content_title, R.string.help_3_content, R.drawable.ic_open_map, false)
			};

			HelpEntry[] helpEntries4 = {
					newHelpEntry(R.string.help_4_gets_title, R.string.help_4_gets, 0, false),
					newHelpEntry(R.string.help_4_web_title, R.string.help_4_web, 0, false),
					newHelpEntry(R.string.help_4_google_title, R.string.help_4_google, 0, false),
					newHelpEntry(R.string.help_4_share_title, R.string.help_4_share, R.drawable.ic_action_share, false),
			};


			switch (position) {
			case 0:
				return PlaceholderFragment.newLayoutInstance(helpEntries1);
			case 1:
				return PlaceholderFragment.newLayoutInstance(helpEntries2);
			case 2:
				return PlaceholderFragment.newLayoutInstance(helpEntries3);
			case 3:
				return PlaceholderFragment.newLayoutInstance(helpEntries4);

			default:
				return PlaceholderFragment.newLayoutInstance(helpEntries1);
			}
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);
			case 3:
				return getString(R.string.title_section4).toUpperCase(l);

			}
			return null;
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		private static final String ARG_ENTRIES = "layout_number";

		public static Fragment newLayoutInstance(HelpEntry[] entries) {
			ArrayList<HelpEntry> entriesArrList = new ArrayList<>();
			Collections.addAll(entriesArrList, entries);

			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();

			args.putSerializable(ARG_ENTRIES, entriesArrList);

			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState) {
			List<HelpEntry> entires = (List) getArguments().getSerializable(ARG_ENTRIES);

			ScrollView scrollView = new ScrollView(getActivity());
			scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			scrollView.setHorizontalScrollBarEnabled(false);
			scrollView.setVerticalScrollBarEnabled(false);

			LinearLayout linearLayout = new LinearLayout(getActivity());
			linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			linearLayout.setOrientation(LinearLayout.VERTICAL);

			scrollView.addView(linearLayout);

			for (HelpEntry entry : entires) {
				View entryView = inflater.inflate(R.layout.help_template, linearLayout, false);

				TextView titleView = (TextView) entryView.findViewById(R.id.help_template_title);
				TextView textView = (TextView) entryView.findViewById(R.id.help_template_text);
				ImageView iconView = (ImageView) entryView.findViewById(R.id.help_template_icon);

				titleView.setText(entry.title);
				textView.setText(entry.text);

				if (entry.iconRes != 0) {
					iconView.setImageResource(entry.iconRes);
					iconView.setVisibility(View.VISIBLE);

					if (entry.reversed) {
						iconView.setColorFilter(0xff515151, PorterDuff.Mode.SRC_ATOP);
					}
				}


				linearLayout.addView(entryView);
			}

			return scrollView;
		}
	}

	private static class HelpEntry implements Serializable {
		HelpEntry(String title, String text, int iconRes, boolean reversed) {
			this.title = title;
			this.text = text;
			this.iconRes = iconRes;
			this.reversed = reversed;
		}

		String title;
		String text;
		int iconRes;
		boolean reversed;
	}

}
