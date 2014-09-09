package org.fruct.oss.socialnavigator.fragments.overlays;

import android.os.Bundle;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class PositionOverlayFragment extends OverlayFragment {
	@Override
	public void onMapViewReady(MapView mapView) {
		GpsMyLocationProvider provider = new GpsMyLocationProvider(getActivity());

		MyLocationNewOverlay overlay = new MyLocationNewOverlay(getActivity(), provider, mapView);
		overlay.enableMyLocation();
		overlay.enableFollowLocation();

		mapView.getOverlayManager().add(overlay);
	}
}
