package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OverlayFragment extends Fragment {
	protected static final Logger log = LoggerFactory.getLogger(OverlayFragment.class);

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

	public abstract void onMapViewReady(MapView mapView);

}
