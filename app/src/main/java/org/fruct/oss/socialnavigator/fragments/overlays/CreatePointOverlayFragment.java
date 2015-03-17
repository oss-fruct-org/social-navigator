package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.TrackPath;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CreatePointOverlayFragment extends OverlayFragment implements PopupMenu.OnMenuItemClickListener, RoutingService.Listener {
	private static final Logger log = LoggerFactory.getLogger(CreatePointOverlayFragment.class);

	private PlaceOverlay placeOverlay;

	private GeoPoint selectedPoint;
	private MapView mapView;

	private PointsConnection pointsServiceConnection;
	private PointsService pointsService;

	private RoutingConnection routingServiceConnection;
	private RoutingService routingService;

	private EditText titleEdit;
	private View view;

	private List<Category> categories;
	private RoutingService.TrackingState trackingState;

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_overlay_create_point, container, false);

		titleEdit = (EditText) view.findViewById(R.id.text_title);

		final Spinner difficultySpinner = (Spinner) view.findViewById(R.id.spinner_difficulty);
		difficultySpinner.setAdapter(createDifficultySpinnerAdapter());

		final Spinner categoriesSpinner = (Spinner) view.findViewById(R.id.spinner_category);

		ImageButton acceptButton = (ImageButton) view.findViewById(R.id.create_button_accept);
		ImageButton closeButton = (ImageButton) view.findViewById(R.id.create_button_close);

		acceptButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String title = titleEdit.getText().toString();
				int difficulty = Integer.parseInt(((String) difficultySpinner.getSelectedItem()));
				Category category = categories.get(categoriesSpinner.getSelectedItemPosition());

				createPoint(title, difficulty, selectedPoint, category);

				mapView.getOverlayManager().remove(placeOverlay);
				selectedPoint = null;
				hidePanel();
				mapView.invalidate();
			}
		});

		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mapView.getOverlayManager().remove(placeOverlay);
				selectedPoint = null;
				hidePanel();
				mapView.invalidate();
			}
		});

		return view;
	}

	private void createPoint(String title, int difficulty, GeoPoint geoPoint, Category category) {
		log.info("Creating point {} with difficulty {} category {}", title, difficulty, category.getName());

		org.fruct.oss.socialnavigator.points.Point point
				= new org.fruct.oss.socialnavigator.points.Point(title, "User created point", "http://example.com",
				geoPoint.getLatitude(), geoPoint.getLongitude(),
				category,org.fruct.oss.socialnavigator.points. Point.LOCAL_PROVIDER,
				"point-" + geoPoint.toString(), difficulty);
		point.setPrivate(true);

		if (pointsService != null) {
			pointsService.addPoint(point);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (routingService != null) {
			routingService.removeListener(this);
		}

		if (pointsServiceConnection != null) {
			getActivity().unbindService(pointsServiceConnection);
			pointsServiceConnection = null;
		}

		if (routingServiceConnection != null) {
			getActivity().unbindService(routingServiceConnection);
			routingServiceConnection = null;
		}
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		Context context = getActivity();
		EventOverlay eventOverlay = new EventOverlay(context);
		mapView.getOverlayManager().add(eventOverlay);

		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection = new PointsConnection(), Context.BIND_AUTO_CREATE);

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection = new RoutingConnection(), Context.BIND_AUTO_CREATE);
	}

	// Routing callbacks
	@Override
	public void routingStateChanged(RoutingService.State state) {
		if (state != RoutingService.State.TRACKING) {
			trackingState = null;
		}
	}

	@Override
	public void progressStateChanged(boolean isActive) {
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
	}

	@Override
	public void activePathUpdated(RoutingService.TrackingState trackingState) {
		this.trackingState = trackingState;
	}


	private void onPointsServiceConnected(PointsService service) {
		pointsService = service;

		Spinner categorySpinner = (Spinner) view.findViewById(R.id.spinner_category);

		ArrayAdapter<String> categorySpinnerAdapter = createCategorySpinnerAdapter();
		categorySpinner.setAdapter(categorySpinnerAdapter);

		categories = pointsService.queryList(pointsService.requestCategories());
		for (Category category : categories) {
			categorySpinnerAdapter.add(category.getDescription());
		}
	}

	private void onPointsServiceDisconnected() {
		pointsService = null;
	}

	private void onRoutingServiceConnected(RoutingService service) {
		routingService = service;
		routingService.addListener(this);
	}

	private void onRoutingServiceDisconnected() {
		routingService = null;
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
		if (routingService != null) {
			routingService.setTargetPoint(selectedPoint);
		}
	}

	private void place() {
		if (routingService != null) {
			routingService.forceLocation(selectedPoint);
		}
	}

	private void createPoint() {
		/*((ActionBarActivity) getActivity()).startSupportActionMode(new CreatePointActionMode());*/

		TrackPath<org.fruct.oss.socialnavigator.points.Point> trackingPath;
		if (trackingState != null) {
			trackingPath = trackingState.trackingPath;
		} else {
			trackingPath = null;
		}

		mapView.getOverlayManager().add(placeOverlay = new PlaceOverlay(getActivity(), selectedPoint, trackingPath));
		mapView.invalidate();
		showPanel();
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

	private void showPanel() {
		if (view.getVisibility() != View.GONE)
			return;

		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);

		if (view != null) {
			view.startAnimation(anim);
			view.setVisibility(View.VISIBLE);
		}
	}

	private void hidePanel() {
		if (view.getVisibility() == View.GONE)
			return;

		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
		anim.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				if (view != null) {
					view.setVisibility(View.GONE);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});

		if (view != null) {
			view.startAnimation(anim);
		}
	}

	private SpinnerAdapter createDifficultySpinnerAdapter() {
		String[] arr = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, arr);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return arrayAdapter;
	}

	private ArrayAdapter<String> createCategorySpinnerAdapter() {
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		return arrayAdapter;
	}

	private class PlaceOverlay extends Overlay {
		private final int ITEM_SIZE = Utils.getDP(48);
		private final int CIRCLE_SIZE = Utils.getDP(8);

		private final GeoPoint geoPoint;
		private final GeoPoint geoPointProjected = new GeoPoint(0, 0);

		private final Drawable drawable;

		private final Point point = new Point();
		private final TrackPath<org.fruct.oss.socialnavigator.points.Point> trackPath;

		private Paint projectedPaint;
		private Paint mainPaint;

		private boolean isDragging;
		private int hookX;
		private int hookY;

		private int dragStartX;
		private int dragStartY;

		public PlaceOverlay(Context ctx, GeoPoint initialPoint, TrackPath<org.fruct.oss.socialnavigator.points.Point> trackPath) {
			super(ctx);
			this.trackPath = trackPath;

			this.geoPoint = new GeoPoint(initialPoint);
			this.drawable = ctx.getResources().getDrawable(R.drawable.blank);

			this.projectedPaint = new Paint();
			projectedPaint.setAntiAlias(true);
			projectedPaint.setColor(0xff4432ff);

			this.mainPaint = new Paint();
			mainPaint.setAntiAlias(true);
			mainPaint.setColor(0x334432ff);
		}

		@Override
		protected void draw(Canvas c, MapView mapView, boolean shadow) {
			Projection proj = mapView.getProjection();

			/*proj.toPixels(geoPoint, point);
			drawable.setBounds(point.x - ITEM_SIZE, point.y - 2 * ITEM_SIZE, point.x + ITEM_SIZE, point.y);
			drawable.draw(c);*/

			proj.toPixels(geoPoint, point);
			c.drawCircle(point.x, point.y, ITEM_SIZE, mainPaint);
			c.drawCircle(point.x, point.y, CIRCLE_SIZE, projectedPaint);

			if (trackPath != null) {
				proj.toPixels(geoPointProjected, point);
				c.drawCircle(point.x, point.y, CIRCLE_SIZE, projectedPaint);
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent e, MapView mapView) {
			if (e.getAction() == MotionEvent.ACTION_DOWN) {
				Projection proj = mapView.getProjection();
				Rect screenRect = proj.getIntrinsicScreenRect();

				dragStartX = (int) e.getX();
				dragStartY = (int) e.getY();

				final int absX = screenRect.left + dragStartX;
				final int absY = screenRect.top + dragStartY;

				proj.toPixels(geoPoint, point);

				if (dragStartX < point.x - ITEM_SIZE || dragStartY < point.y - ITEM_SIZE
						|| dragStartX > point.x + ITEM_SIZE || dragStartY > point.y + ITEM_SIZE) {
					return false;
				}

				hookX = point.x - absX;
				hookY = point.y - absY;

				isDragging = true;
				return true;
			} else if (isDragging && e.getAction() == MotionEvent.ACTION_MOVE) {
				Projection proj = mapView.getProjection();
				proj.fromPixels((int) e.getX() + hookX, (int) e.getY() + hookY, geoPoint);
				moveSelectedPoint(geoPoint);
				mapView.invalidate();
				return true;
			} else if (isDragging && e.getAction() == MotionEvent.ACTION_UP) {
				isDragging = false;
				if (trackPath == null) {
					selectedPoint = new GeoPoint(geoPoint);
				} else {
					selectedPoint = new GeoPoint(geoPointProjected);
				}
				return true;
			}

			return super.onTouchEvent(e, mapView);
		}

		private void moveSelectedPoint(GeoPoint geoPoint) {
			geoPoint.setLatitudeE6(geoPoint.getLatitudeE6());
			geoPoint.setLongitudeE6(geoPoint.getLongitudeE6());

			if (trackPath != null) {
				// TODO: can be optimized
				TrackPath.ProjectedPoint projectedPoint
						= trackPath.projectPoint(geoPoint.getLatitude(),
						geoPoint.getLongitude());

				geoPointProjected.setCoordsE6((int) (projectedPoint.getProjX() * 1e6),
						(int) (projectedPoint.getProjY() * 1e6));
			}
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
		public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
			processPointClick(e, mapView);
			return super.onSingleTapConfirmed(e, mapView);
		}

		@Override
		public boolean onLongPress(MotionEvent e, MapView mapView) {
			processPointClick(e, mapView);
			return super.onLongPress(e, mapView);
		}

		private void processPointClick(MotionEvent e, MapView mapView) {
			Projection projection = mapView.getProjection();
			IGeoPoint geoPoint = projection.fromPixels((int) e.getX(), (int) e.getY());

			Point screenPoint = new Point((int) e.getX(), (int) e.getY());
			screenPoint.offset(-projection.getScreenRect().left, -projection.getScreenRect().top);

			onPointPressed(geoPoint, screenPoint);
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

	private class RoutingConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (routingServiceConnection != null) {
				onRoutingServiceConnected(((RoutingService.Binder) service).getService());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			onRoutingServiceDisconnected();
		}

	}
}
