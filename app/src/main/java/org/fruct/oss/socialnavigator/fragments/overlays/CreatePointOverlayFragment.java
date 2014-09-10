package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.widget.PopupMenuCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.R;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePointOverlayFragment extends OverlayFragment implements PopupMenu.OnMenuItemClickListener {
	private static final Logger log = LoggerFactory.getLogger(CreatePointOverlayFragment.class);

	private EventOverlay overlay;

	@Override
	public void onMapViewReady(MapView mapView) {
		overlay = new EventOverlay(getActivity());
		mapView.getOverlayManager().add(overlay);
	}

	private void onPointPressed(IGeoPoint geoPoint, Point screenPoint) {
		log.info("Map pressed on {}, {}", geoPoint.getLatitude(), geoPoint.getLongitude());

		View anchorView = getActivity().findViewById(R.id.map_anchor);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			anchorView.setTranslationX(screenPoint.x);
			anchorView.setTranslationY(screenPoint.y);
		}

		PopupMenu popupMenu = new PopupMenu(getActivity(), anchorView);
		popupMenu.inflate(R.menu.popup_point);
		popupMenu.setOnMenuItemClickListener(this);
		popupMenu.show();
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
		case R.id.action_route:

			break;

		case R.id.action_create:
			Toast.makeText(getActivity(), R.string.str_not_implemented, Toast.LENGTH_SHORT).show();
			break;

		default:
			return false;
		}

		return true;
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

}
