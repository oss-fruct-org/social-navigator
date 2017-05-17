package org.fruct.oss.socialnavigator.fragments.root;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
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
import org.fruct.oss.socialnavigator.audiblealert.AudioManager;
import org.fruct.oss.socialnavigator.fragments.overlays.CreatePointOverlayFragment;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.widget.FrameLayout.LayoutParams;

public class MapFragment extends Fragment implements RoutingService.Listener {
	private static final Logger log = LoggerFactory.getLogger(MapFragment.class);

	private static final String STATE = "state";
	private static final String STATE_FOLLOW = "is-following-active";
	public static final String STORED_LAT = "pref-map-fragment-center-lat";
	public static final String STORED_LON = "pref-map-fragment-center-lon";
	public static final String STORED_ZOOM = "pref-map-fragment-zoom";

	private MapView mapView;
	private List<OverlayFragment> overlayFragments = new ArrayList<OverlayFragment>();

	// звуковое сопровождение
	private AudioManager audioManager;
	private boolean isFollowingActive;

	// отслеживание изменений маршрута для выдачи звуковых уведомлений
	private RoutingService routingService;
	private RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private Space space = new EarthSpace();
	private Turn lastTurn = null;
	private Point lastPoint = null;

	private State state = new State();

	private Point initialScrollPoint;

	// наш сервер с картами
	private static final OnlineTileSourceBase OWN_TILES = new XYTileSource(
			"OSMWithoutSidewalks", ResourceProxy.string.mapnik, 0, 17, 256, ".png",
			new String[] { "http://etourism.cs.petrsu.ru:20209/osm_tiles/" });

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
			isFollowingActive = savedInstanceState.getBoolean(STATE_FOLLOW);
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

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);

		audioManager = new AudioManager(this.getActivity().getApplicationContext());
		if (isFollowingActive) {
			audioManager.startPlaying();
		}

		log.debug("onCreate");
	}

	@Override
	public void onDestroy() {
		audioManager.onDestroy();

		getActivity().unbindService(routingServiceConnection);

		if (routingService != null) {
			routingService.removeListener(this);
		}

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
		if (item.getItemId() == R.id.action_position) {
			log.debug("Cache following button pressed " + isFollowingActive);
			isFollowingActive = !isFollowingActive;
			if (isFollowingActive) {
				audioManager.startPlaying();
			} else {
				audioManager.stopPlaying();
			}
			return false;
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
		//ITileSource tileSource = TileSourceFactory.MAPQUESTOSM;
		//ITileSource tileSource = TileSourceFactory.MAPNIK;
		ITileSource tileSource = OWN_TILES;
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
		mapView.getController().setZoom(15);


		layout.addView(mapView);
		//setHardwareAccelerationOff();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setHardwareAccelerationOff() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
	}

	@Override
	public void routingStateChanged(RoutingService.State state) {
	}

	@Override
	public void progressStateChanged(boolean isActive) {
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
	}

	@Override
	public void activePathUpdated(RoutingService.TrackingState trackingState) {
		TrackPath.Result<Point> lastQueryResult = trackingState.lastQueryResult;
		// препятствие в маршруте
		if (lastQueryResult.nextPointData != null) {
			float[] dist = new float[1];
			Location.distanceBetween(
					lastQueryResult.nextPointData.getLat(),
					lastQueryResult.nextPointData.getLon(),
					lastQueryResult.currentPosition.x,
					lastQueryResult.currentPosition.y, dist);
			if (dist[0] < TrackingOverlayFragment.OBJECT_PROXIMITY_NOTIFICATION) {
				if (this.lastPoint == null || !this.lastPoint.equals(lastQueryResult.nextPointData)) {
					this.lastPoint = lastQueryResult.nextPointData;
					audioManager.queueToPlay(lastQueryResult.nextPointData, AudioManager.FORWARD);
					audioManager.playNext();
				}
			}
		}

		// направление движения
		if (lastQueryResult.nextTurn != null
				&& lastQueryResult.nextTurn.getTurnSharpness() > 1) {
			double dist = space.dist(lastQueryResult.nextTurn.getPoint(),
					lastQueryResult.currentPosition);

			if (dist < TrackingOverlayFragment.TURN_PROXIMITY_NOTIFICATION) {
				if (this.lastTurn == null || !this.lastTurn.equals(lastQueryResult.nextTurn)) {
					this.lastTurn = lastQueryResult.nextTurn;

					if (lastQueryResult.nextTurn.getTurnDirection() > 0) {
						audioManager.queueTurnToPlay(AudioManager.TO_LEFT);
					} else {
						audioManager.queueTurnToPlay(AudioManager.TO_RIGHT);
					}
					audioManager.playNext();
				}
			} else {
				//log.debug("distance = " + dist);
			}
		}
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

	private class RoutingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			routingService = ((RoutingService.Binder) service).getService();
			routingService.addListener(MapFragment.this);
			routingService.sendLastResult();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			routingService = null;
		}
	}
}
