package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import org.fruct.oss.socialnavigator.R;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class PositionOverlayFragment extends OverlayFragment {
	private static final String STATE_FOLLOW = "is-following-active";

	private MyLocationNewOverlay overlay;
	private MenuItem positionMenuItem;

	private boolean isFollowingActive;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		setHasOptionsMenu(true);

		if (in != null) {
			isFollowingActive = in.getBoolean(STATE_FOLLOW);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map_position, menu);

		positionMenuItem = menu.findItem(R.id.action_position);

		if (isFollowingActive) {
			positionMenuItem.getIcon().setColorFilter(0xff669900, PorterDuff.Mode.SRC_ATOP);
		} else {
			positionMenuItem.getIcon().setColorFilter(null);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_position) {
			activateFollowMode();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void activateFollowMode() {
		isFollowingActive = true;
		overlay.enableFollowLocation();
		//positionMenuItem.setIcon(R.drawable.ic_action_location_found);
		if (positionMenuItem != null)
			positionMenuItem.getIcon().setColorFilter(0xff669900, PorterDuff.Mode.SRC_ATOP);
	}

	private void deactivateFollowMode() {
		isFollowingActive = false;
		overlay.disableFollowLocation();
		positionMenuItem.getIcon().setColorFilter(null);
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		GpsMyLocationProvider provider = new GpsMyLocationProvider(getActivity());

		overlay = new ScrollableOverlay(getActivity(), provider, mapView);
		overlay.enableMyLocation();

		if (isFollowingActive)
			activateFollowMode();

		mapView.getOverlayManager().add(overlay);
	}

	@Override
	public void onSaveInstanceState(Bundle out) {
		super.onSaveInstanceState(out);

		out.putBoolean(STATE_FOLLOW, isFollowingActive);
	}

	private class ScrollableOverlay extends MyLocationNewOverlay {
		public ScrollableOverlay(Context context, IMyLocationProvider myLocationProvider, MapView mapView) {
			super(context, myLocationProvider, mapView);
		}

		@Override
		public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
			deactivateFollowMode();
			return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView);
		}
	}
}
