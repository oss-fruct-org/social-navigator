package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayFragment extends Fragment {
	private static final Logger log = LoggerFactory.getLogger(OverlayFragment.class);

	private Context context;

	public OverlayFragment() {
	}

	public void onCreate(Bundle in) {
		super.onCreate(in);
		log.debug("onCreate {}", in);
		context = getActivity();
	}

	public void onDestroy() {
		super.onDestroy();
		log.debug("onDestroy");
	}

	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);
		log.debug("onSaveInstanceState");
	}

	public void onMapViewReady(MapView mapView) {
		GpsMyLocationProvider provider = new GpsMyLocationProvider(context);

		MyLocationNewOverlay overlay = new MyLocationNewOverlay(context, provider, mapView);
		mapView.getOverlayManager().add(overlay);
		overlay.enableMyLocation();
		overlay.enableFollowLocation();

		mapView.getOverlayManager().add(overlay);
	}
}
