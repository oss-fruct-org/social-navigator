package org.fruct.oss.socialnavigator.fragments.root;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.fragments.overlays.OverlayHolder;
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
import org.slf4j.spi.LoggerFactoryBinder;

import java.util.ArrayList;
import java.util.List;

import static android.widget.FrameLayout.LayoutParams;

public class MapFragment extends Fragment {
	private static final Logger log = LoggerFactory.getLogger(MapFragment.class);

	private static final String STATE = "state";

	private MapView mapView;
	private FrameLayout mapLayout;
	private List<OverlayHolder> overlayHolders = new ArrayList<OverlayHolder>();

	private State state = new State();

	public MapFragment() {
	}

	public static MapFragment newInstance() {
		return new MapFragment();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_map, container, false);

		createMapView(view);

		if (savedInstanceState != null) {
			state = savedInstanceState.getParcelable(STATE);
		} else {
			state.zoom = 15;
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
		OverlayHolder overlayFragment = new OverlayHolder(getActivity());
		overlayHolders.add(overlayFragment);

		int c = 0;
		for (OverlayHolder overlay : overlayHolders) {
			if (savedInstanceState != null) {
				Bundle overlayState = savedInstanceState.getBundle("overlay-holder-" + c++);
				overlay.onCreate(overlayState);
			} else {
				overlay.onCreate(null);
			}
		}
	}

	private void onGlobalLayout() {
		mapView.getController().setZoom(state.zoom);
		mapView.getController().setCenter(new GeoPoint(state.lat, state.lon));

		// Notify all existing overlay fragments about mapView
		for (OverlayHolder overlay : overlayHolders) {
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log.debug("onCreate" ,"qwe");
	}

	@Override
	public void onDestroy() {
		for (OverlayHolder overlay : overlayHolders) {
			overlay.onDestroy();
		}

		log.debug("onDestroy");
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelable(STATE, state);

		int c = 0;
		for (OverlayHolder overlay : overlayHolders) {
			Bundle overlayState = new Bundle();
			overlay.onSaveInstanceState(overlayState);
			outState.putBundle("overlay-holder-" + c++, overlayState);
		}
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
