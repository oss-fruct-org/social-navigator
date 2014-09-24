package org.fruct.oss.socialnavigator.fragments.overlays;

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
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.List;

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

		overlay.disableMyLocation();
		getActivity().unbindService(routingServiceConnection);
		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
		this.routingService.sendLastLocation();
	}

	private void onRoutingServiceDisconnected() {
		routingService = null;
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, List<RoutingService.Path> paths) {

	}

	@Override
	public void pathsCleared() {

	}

	/*@Override
	public void routingUpdated(RoutingService.Path path) {
		if (path != null) {
			GeoPoint p1 = new GeoPoint(path.getPointList().getLatitude(0), path.getPointList().getLongitude(0));
			GeoPoint p2 = new GeoPoint(path.getPointList().getLatitude(1), path.getPointList().getLongitude(1));

			double bearing = p1.bearingTo(p2);
			mapView.setMapOrientation((float) -bearing);

			activateFollowMode();
		} else {
			deactivateFollowMode();
		}
	}*/

	private class ScrollableOverlay extends MyLocationNewOverlay {
		public ScrollableOverlay(Context context, IMyLocationProvider myLocationProvider, MapView mapView) {
			super(context, myLocationProvider, mapView);
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
			LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
			receiver = null;
		}

		@Override
		public Location getLastKnownLocation() {
			return location;
		}
	}
}
