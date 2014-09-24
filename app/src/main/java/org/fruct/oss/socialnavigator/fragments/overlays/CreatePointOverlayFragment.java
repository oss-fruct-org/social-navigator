package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.dialogs.CreatePointDialog;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePointOverlayFragment extends OverlayFragment implements PopupMenu.OnMenuItemClickListener, CreatePointDialog.Listener {
	private static final Logger log = LoggerFactory.getLogger(CreatePointOverlayFragment.class);

	private EventOverlay overlay;

	private GeoPoint selectedPoint;
	private MapView mapView;

	private PointsConnection pointsServiceConnection;
	private PointsService pointsService;
	private CreatePointActionMode actionModeCallback;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		CreatePointDialog createPointDialog = (CreatePointDialog) getFragmentManager().findFragmentByTag("create-point-dialog");
		if (createPointDialog != null) {
			createPointDialog.setListener(this);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (pointsServiceConnection != null) {
			getActivity().unbindService(pointsServiceConnection);
			pointsServiceConnection = null;
		}
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		Context context = getActivity();
		overlay = new EventOverlay(context);
		mapView.getOverlayManager().add(overlay);


		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection = new PointsConnection(), Context.BIND_AUTO_CREATE);
	}


	private void onPointsServiceConnected(PointsService service) {
		pointsService = service;
	}

	private void onPointsServiceDisconnected() {
		pointsService = null;
	}

	private void onPointPressed(IGeoPoint geoPoint, Point screenPoint) {
		log.info("Map pressed on {}, {}", geoPoint.getLatitude(), geoPoint.getLongitude());

		View anchorView = getActivity().findViewById(R.id.map_anchor);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			anchorView.setTranslationX(screenPoint.x);
			anchorView.setTranslationY(screenPoint.y);
		}

		PopupMenu popupMenu = new PopupMenu(getActivity(), anchorView);
		popupMenu.inflate(R.menu.popup_point);
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();

		selectedPoint = new GeoPoint(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
	}

	private void route() {
		Intent intent = new Intent(RoutingService.ACTION_ROUTE, null, getActivity(), RoutingService.class);
		intent.putExtra(RoutingService.ARG_POINT, (Parcelable) selectedPoint);
		getActivity().startService(intent);
	}

	private void place() {
		Intent intent = new Intent(RoutingService.ACTION_PLACE, null, getActivity(), RoutingService.class);
		intent.putExtra(RoutingService.ARG_POINT, (Parcelable) selectedPoint);
		getActivity().startService(intent);
	}

	private void createPoint() {
		/*
		CreatePointDialog dialog = CreatePointDialog.newInstance(selectedPoint);
		dialog.setListener(this);
		dialog.show(getFragmentManager(), "create-point-dialog");
		*/

		((ActionBarActivity) getActivity()).startSupportActionMode(actionModeCallback = new CreatePointActionMode());
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.action_route:
			route();
			break;

		case R.id.action_create:
			createPoint();
			break;

		case R.id.action_place_here:
			place();
			break;

		default:
			return false;
		}

		return true;
	}

	@Override
	public void pointCreated(org.fruct.oss.socialnavigator.points.Point point) {
		if (pointsService != null) {
			pointsService.addPoint(point);
		}
	}

	private class CreatePointActionMode implements ActionMode.Callback {
		private boolean isCancelled = false;

		@Override
		public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
			MenuInflater inflater = actionMode.getMenuInflater();
			inflater.inflate(R.menu.action_mode_create_point, menu);
			actionMode.setTitle(R.string.str_point_action_mode);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
			if (menuItem.getItemId() == R.id.action_cancel) {
				isCancelled = true;
			}
			actionMode.finish();
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode actionMode) {
			if (isCancelled) {
				Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getActivity(), "Accepted", Toast.LENGTH_LONG).show();
			}
		}
	}

	private class PlaceOverlay extends Overlay {
		public PlaceOverlay(Context ctx) {
			super(ctx);
		}

		@Override
		protected void draw(Canvas c, MapView mapView, boolean shadow) {

		}
	}

	private class EventOverlay extends Overlay {
		public EventOverlay(Context ctx) {
			super(ctx);
		}

		@Override
		protected void draw(Canvas c, MapView osmv, boolean shadow) {
		}

		@Override
		public boolean onLongPress(MotionEvent e, MapView mapView) {
			Projection projection = mapView.getProjection();
			IGeoPoint geoPoint = projection.fromPixels((int) e.getX(), (int) e.getY());

			Point screenPoint = new Point((int) e.getX(), (int) e.getY());
			screenPoint.offset(-projection.getScreenRect().left, -projection.getScreenRect().top);

			onPointPressed(geoPoint, screenPoint);
			return super.onLongPress(e, mapView);
		}
	}

	private class PointsConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (pointsServiceConnection != null) {
				onPointsServiceConnected(((PointsService.Binder) service).getService());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onPointsServiceDisconnected();
		}
	}
}
