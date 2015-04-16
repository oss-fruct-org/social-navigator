package org.fruct.oss.socialnavigator.fragments.root;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.fragments.overlays.CreatePointOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.ObstaclesOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.OverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.PositionOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.RouteOverlayFragment;
import org.fruct.oss.socialnavigator.fragments.overlays.TrackingOverlayFragment;
import org.fruct.oss.socialnavigator.points.Point;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static android.widget.FrameLayout.LayoutParams;

public class MapFragment extends Fragment {
	private static final Logger log = LoggerFactory.getLogger(MapFragment.class);

	private static final String STATE = "state";
	public static final String STORED_LAT = "pref-map-fragment-center-lat";
	public static final String STORED_LON = "pref-map-fragment-center-lon";
	public static final String STORED_ZOOM = "pref-map-fragment-zoom";

	private MapView mapView;
	private List<OverlayFragment> overlayFragments = new ArrayList<OverlayFragment>();

	private State state = new State();

	private Point initialScrollPoint;

	public MapFragment() {
	}

	public static MapFragment newInstance() {
		return new MapFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section1), ActionBar.NAVIGATION_MODE_STANDARD, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_map, container, false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

		createMapView(view);

		if (savedInstanceState != null) {
			state = savedInstanceState.getParcelable(STATE);
		} else {
			state.lat = pref.getInt(STORED_LAT, 0) / 1e6;
			state.lon = pref.getInt(STORED_LON, 0) / 1e6;
			state.zoom = pref.getInt(STORED_ZOOM, 15);
		}

		if (savedInstanceState == null && getArguments() != null) {
			initialScrollPoint = getArguments().getParcelable("point");
		}

		setupOverlays(savedInstanceState);

		ViewTreeObserver vto = view.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				ViewTreeObserver vto = view.getViewTreeObserver();
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
					vto.removeGlobalOnLayoutListener(this);
				} else {
					vto.removeOnGlobalLayoutListener(this);
				}
				MapFragment.this.onGlobalLayout();
			}
		});

		return view;
	}

	private void setupOverlays(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			FragmentTransaction trans = getFragmentManager().beginTransaction();

			PositionOverlayFragment positionOverlayFragment = new PositionOverlayFragment();
			trans.add(positionOverlayFragment, "position-overlay-fragment");
			overlayFragments.add(positionOverlayFragment);

			ObstaclesOverlayFragment obstaclesOverlayFragment = new ObstaclesOverlayFragment();
			trans.add(obstaclesOverlayFragment, "obstacle-overlay-fragment");
			overlayFragments.add(obstaclesOverlayFragment);

			CreatePointOverlayFragment createPointOverlayFragment = new CreatePointOverlayFragment();
			trans.add(R.id.overlay_create_point, createPointOverlayFragment, "create-point-overlay-fragment");
			overlayFragments.add(createPointOverlayFragment);

			RouteOverlayFragment routeOverlayFragment = new RouteOverlayFragment();
			trans.add(R.id.overlay_route, routeOverlayFragment, "route-overlay-fragment");
			overlayFragments.add(routeOverlayFragment);

			TrackingOverlayFragment trackingOverlayFragment = new TrackingOverlayFragment();
			trans.add(R.id.overlay_tracking, trackingOverlayFragment, "tracking-overlay-fragment");
			overlayFragments.add(trackingOverlayFragment);

			trans.addToBackStack(null);
			trans.commit();
		} else {
			int overlayCount = savedInstanceState.getInt("overlay-holder-count");
			for (int i = 0; i < overlayCount; i++) {
				OverlayFragment overlay = (OverlayFragment) getFragmentManager().getFragment(savedInstanceState, "overlay-holder-" + i);
				overlayFragments.add(overlay);
			}
		}
	}

	private void onGlobalLayout() {
		mapView.getController().setZoom(state.zoom);
		mapView.getController().setCenter(new GeoPoint(state.lat, state.lon));

		if (initialScrollPoint != null) {
			mapView.getController().animateTo(initialScrollPoint.toGeoPoint());
		}

		// Notify all existing overlay fragments about mapView
		for (OverlayFragment overlay : overlayFragments) {
			overlay.onMapViewReady(mapView);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		state.zoom = mapView.getZoomLevel();
		state.lat = mapView.getMapCenter().getLatitude();
		state.lon = mapView.getMapCenter().getLongitude();
	}

	@Override
	public void onStop() {
		super.onStop();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		pref.edit().putInt(STORED_LAT, mapView.getMapCenter().getLatitudeE6())
				.putInt(STORED_LON, mapView.getMapCenter().getLongitudeE6())
				.putInt(STORED_ZOOM, mapView.getZoomLevel()).apply();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		log.debug("onCreate");
	}

	@Override
	public void onDestroy() {
		log.debug("onDestroy");
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		inflater.inflate(R.menu.map_fragment_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_open_categories) {
			((MainActivity) getActivity()).openCategoriesFragment();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE, state);

		int c = 0;
		for (OverlayFragment overlay : overlayFragments) {
			getFragmentManager().putFragment(outState, "overlay-holder-" + c++, overlay);
		}
		outState.putInt("overlay-holder-count", overlayFragments.size());
	}


	private void createMapView(View view) {
		ViewGroup layout = (ViewGroup) view.findViewById(R.id.map_view);

		IRegisterReceiver registerReceiver = new SimpleRegisterReceiver(getActivity().getApplicationContext());
		ITileSource tileSource = TileSourceFactory.MAPQUESTOSM;
		TileWriter tileWriter = new TileWriter();
		NetworkAvailabliltyCheck networkAvailabliltyCheck = new NetworkAvailabliltyCheck(getActivity());

		MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(
				registerReceiver, tileSource);
		MapTileDownloader downloaderProvider = new MapTileDownloader(tileSource, tileWriter, networkAvailabliltyCheck);

		MapTileProviderArray tileProviderArray = new MapTileProviderArray(
				tileSource, registerReceiver, new MapTileModuleProviderBase[]{
				fileSystemProvider, downloaderProvider});

		mapView = new MapView(getActivity(), 256, new DefaultResourceProxyImpl(getActivity()), tileProviderArray);
		mapView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mapView.setMultiTouchControls(true);


		layout.addView(mapView);
		//setHardwareAccelerationOff();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	private static class State implements Parcelable {
		double lat;
		double lon;
		int zoom;

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			parcel.writeDouble(lat);
			parcel.writeDouble(lon);
			parcel.writeInt(zoom);
		}

		public static Creator<State> CREATOR = new Creator<State>() {
			@Override
			public State createFromParcel(Parcel parcel) {
				State state = new State();

				state.lat = parcel.readDouble();
				state.lon = parcel.readDouble();
				state.zoom = parcel.readInt();

				return state;
			}

			@Override
			public State[] newArray(int size) {
				return new State[size];
			}
		};
	}
}
