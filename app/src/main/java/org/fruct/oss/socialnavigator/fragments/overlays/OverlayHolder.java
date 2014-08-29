package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.Context;
import android.os.Bundle;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayHolder {
	private static final Logger log = LoggerFactory.getLogger(OverlayHolder.class);
	private final Context context;

	public OverlayHolder(Context context) {
		this.context = context;
	}

	public void onCreate(Bundle in) {
		log.debug("onCreate {}", in);
	}

	public void onDestroy() {
		log.debug("onDestroy");
	}

	public void onSaveInstanceState(Bundle out) {
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
