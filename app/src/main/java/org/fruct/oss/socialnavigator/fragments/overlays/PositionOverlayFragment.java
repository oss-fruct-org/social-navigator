package org.fruct.oss.socialnavigator.fragments.overlays;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Map;

import static org.fruct.oss.socialnavigator.MainActivity.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION;

public class PositionOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private static final String STATE_FOLLOW = "is-following-active";

	private MyLocationNewOverlay overlay;
	private MenuItem positionMenuItem;

	private boolean isFollowingActive;

	private RoutingService routingService;
	private RoutingServiceConnection routingServiceConnection;
	private MapView mapView;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		setHasOptionsMenu(true);

		if (in != null) {
			isFollowingActive = in.getBoolean(STATE_FOLLOW);
		}

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection = new RoutingServiceConnection(), Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		if (routingService != null) {
			routingService.removeListener(this);
		}

		if (overlay != null)
			overlay.disableMyLocation();
		getActivity().unbindService(routingServiceConnection);
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (menu.findItem(R.id.action_position) == null)
			inflater.inflate(R.menu.map_position, menu);

		positionMenuItem = menu.findItem(R.id.action_position);

		if (isFollowingActive) {
			positionMenuItem.getIcon().setColorFilter(getResources().getColor(R.color.color_blue), PorterDuff.Mode.SRC_ATOP);
		} else {
			positionMenuItem.getIcon().setColorFilter(null);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_position) {
			if (isFollowingActive)
				deactivateFollowMode();
			else
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
			positionMenuItem.getIcon().setColorFilter(getResources().getColor(R.color.color_blue), PorterDuff.Mode.SRC_ATOP);
	}

	private void deactivateFollowMode() {
		isFollowingActive = false;
		overlay.disableFollowLocation();
		positionMenuItem.getIcon().setColorFilter(null);
		mapView.setMapOrientation(0);
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;
		IMyLocationProvider provider = new LocationProvider(getActivity());

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

	private void onRoutingServiceConnected(RoutingService routingService) {
		this.routingService = routingService;
		this.routingService.addListener(this);
		try {
			this.routingService.sendLastLocation();
		} catch (SecurityException ex) {
			this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
					MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
		}
	}

	private void onRoutingServiceDisconnected() {
		routingService = null;
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

	}

	private class ScrollableOverlay extends MyLocationNewOverlay {
		public ScrollableOverlay(Context context, IMyLocationProvider myLocationProvider, MapView mapView) {
			super(myLocationProvider, mapView);
		}

		@Override
		public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2, float pDistanceX, float pDistanceY, MapView pMapView) {
			//deactivateFollowMode();
			return super.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				return false;
			} else {
				return super.onTouchEvent(event, mapView);
			}
		}
	}

	private class RoutingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			onRoutingServiceConnected(((RoutingService.Binder) service).getService());
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onRoutingServiceDisconnected();
		}
	}

	private static class LocationProvider implements IMyLocationProvider {
		private final Context context;
		private IMyLocationConsumer consumer;
		private BroadcastReceiver receiver;
		private Location location;

		public LocationProvider(Context context) {
			this.context = context;
		}

		@Override
		public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
			this.consumer = myLocationConsumer;

			LocalBroadcastManager.getInstance(context).registerReceiver(receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					location = intent.getParcelableExtra(RoutingService.ARG_LOCATION);
					consumer.onLocationChanged(location, LocationProvider.this);
				}
			}, new IntentFilter(RoutingService.BC_LOCATION));

			return true;
		}

		@Override
		public void stopLocationProvider() {
			if (receiver != null)
				LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
			receiver = null;
		}

		@Override
		public Location getLastKnownLocation() {
			return location;
		}

		@Override
		public void destroy() {
			if (receiver != null)
				LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
		}
	}
}
