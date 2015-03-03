package org.fruct.oss.socialnavigator.fragments.overlays;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.PathPointList;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RouteOverlayFragment extends OverlayFragment implements RoutingService.Listener, AdapterView.OnItemClickListener {
	private static final Logger log = LoggerFactory.getLogger(RouteOverlayFragment.class);

	private final RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private final PointsServiceConnection pointsServiceConnection = new PointsServiceConnection();

	private Preferences appPreferences;

	private PointsService pointsService;
	private RoutingService routingService;

	private int servicesBoundCount;

	private final List<PathOverlay> pathOverlays = new ArrayList<PathOverlay>();
	private ItemizedIconOverlay<TargetPointItem> targetPointOverlay;
	private GeoPoint targetPoint;

	private Point[] currentListPoints;
	private Map<RoutingType, ChoicePath> paths = new EnumMap<RoutingType, ChoicePath>(RoutingType.class);
	private RoutingType activeRoutingType = RoutingType.SAFE;

	private MapView mapView;
	private ResourceProxy resourceProxy;

	private View view;
	private boolean expanded = false;

	private Drawable targetPointDrawable;

	private View.OnClickListener typeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			PopupMenu popupMenu = new PopupMenu(getActivity(), v);

			for (Map.Entry<RoutingType, ChoicePath> entry : paths.entrySet()) {
				final RoutingType routingType = entry.getKey();

				popupMenu.getMenu()
						.add(routingType.getStringId())
						.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						pathSelected(routingType);
						return true;
					}
				});
			}

			popupMenu.show();
		}
	};

	private View.OnClickListener closeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (routingService != null) {
				routingService.clearTargetPoint();
			}
		}
	};

	private View.OnClickListener acceptListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (routingService != null) {
				// FIXME: can be crash if service in wrong state (i.e. updating)
				routingService.activateRoute(activeRoutingType);
			}
		}
	};


	private View.OnClickListener expandListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			View bottomView = view.findViewById(R.id.details_view);
			TextView expandTextView = (TextView) view.findViewById(R.id.expand_text_view);

			if (expanded) {
				Utils.collapse(bottomView);
				expandTextView.setText(R.string.str_expand_panel);
			} else {
				Utils.expand(bottomView);
				expandTextView.setText(R.string.str_collapse_panel);
			}
			expanded = !expanded;
		}
	};

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		appPreferences = new Preferences(getActivity());
		targetPointDrawable = getResources().getDrawable(R.drawable.star);
		activeRoutingType = appPreferences.getActiveRoutingType();
	}

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection, Context.BIND_AUTO_CREATE);

		resourceProxy = new DefaultResourceProxyImpl(getActivity());
	}

	public void onDestroy() {
		getActivity().unbindService(routingServiceConnection);
		getActivity().unbindService(pointsServiceConnection);
		routingService.removeListener(this);

		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_overlay_route, container, false);

		ImageButton closeButton = (ImageButton) view.findViewById(R.id.route_button_close);
		closeButton.setOnClickListener(closeListener);

		ImageButton typeButton = (ImageButton) view.findViewById(R.id.route_button_type);
		typeButton.setOnClickListener(typeListener);

		ImageButton acceptButton = (ImageButton) view.findViewById(R.id.route_button_accept);
		acceptButton.setOnClickListener(acceptListener);

		view.setOnClickListener(expandListener);

		return view;
	}

	private void showPathInfo(ChoicePath path) {
		TextView lengthTextView = (TextView) view.findViewById(R.id.length_text);
		TextView titleTextView = (TextView) view.findViewById(R.id.title_text);
		ListView obstaclesListView = (ListView) view.findViewById(R.id.obstacles_list_view);

		if (path != null) {
			lengthTextView.setVisibility(View.VISIBLE);
			lengthTextView.setText(Utils.stringDistance(getResources(), path.getDistance()));
			titleTextView.setText(path.getRoutingType().getStringId());

			List<String> obstaclesList = new ArrayList<String>();
			for (Point point : path.getPoints()) {
				obstaclesList.add(point.getName());
			}

			this.currentListPoints = path.getPoints();

			ArrayAdapter<String> obstaclesAdapter = new ArrayAdapter<String>(getActivity(),
					android.R.layout.simple_list_item_1, obstaclesList);
			obstaclesListView.setAdapter(obstaclesAdapter);
			obstaclesListView.setOnItemClickListener(this);
		} else {
			lengthTextView.setText(getResources().getString(R.string.str_path_not_found));
			lengthTextView.setVisibility(View.GONE);
		}
	}

	private void pathSelected(RoutingType routingType) {
		changeActiveRoutingType(routingType);
		updateOverlays();
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

	private void onServicesReady() {
		routingService.addListener(this);
		routingService.sendLastResult();
	}

	private void onServicesDisconnected() {
		routingService.removeListener(this);
	}

	private void createOverlay(ChoicePath path) {
		PathOverlay pathOverlay = new PathOverlay(getColorByPathType(path), 8, resourceProxy);
		pathOverlay.setAlpha(path.getRoutingType() == activeRoutingType ? 255 : 50);

		PointList points = path.getResponse().getPoints();
		for (int i = 0; i < points.size(); i++) {
			GeoPoint geoPoint = new GeoPoint(points.getLatitude(i),
					points.getLongitude(i));
			pathOverlay.addPoint(geoPoint);
		}

		pathOverlays.add(pathOverlay);
		mapView.getOverlayManager().add(pathOverlay);
	}

	private void updateOverlays() {
		mapView.getOverlayManager().removeAll(pathOverlays);
		mapView.getOverlayManager().remove(targetPointOverlay);
		pathOverlays.clear();

		ChoicePath currentPath = paths.get(activeRoutingType);
		for (ChoicePath path : paths.values()) {
			if (path == currentPath)
				continue;

			createOverlay(path);
		}

		if (currentPath != null) {
			createOverlay(currentPath);
			showPathInfo(currentPath);
		}

		// Create target point overlay
		targetPointOverlay = new ItemizedIconOverlay<TargetPointItem>(new ArrayList<TargetPointItem>(),
				null, new DefaultResourceProxyImpl(getActivity()));
		targetPointOverlay.addItem(new TargetPointItem(targetPoint, targetPointDrawable));
		mapView.getOverlayManager().add(targetPointOverlay);

		mapView.invalidate();
	}

	private void showEventNotification(String notification) {
		/*PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity());
		builder.setContentTitle("Social navigator")
				.setContentText(notification)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify("event", 0, builder.build());*/

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setCancelable(false)
				.setTitle("Notification")
				.setMessage(notification);

		final AlertDialog alertDialog = builder.create();
		alertDialog.show();

		view.postDelayed(new Runnable() {
			@Override
			public void run() {
				alertDialog.dismiss();
			}
		}, 1000);
	}

	@Override
	public void proximityEvent(Point point) {
		showEventNotification(getResources().getString(R.string.str_approaching, point.getName()));
	}

	@Override
	public void proximityEvent(Turn turn) {
		float[] dist = new float[1];

		int turnStrRes = turn.getTurnDirection() > 0
				? R.string.str_turn_left
				: R.string.str_turn_right;

		Location currentLocation = routingService.getLastLocation();
		Location.distanceBetween(turn.getPoint().x, turn.getPoint().y,
				currentLocation.getLatitude(), currentLocation.getLongitude(), dist);

		int intDist = (int) dist[0];
		String str = getResources().getString(turnStrRes,
				getResources().getQuantityString(R.plurals.plural_meters, intDist, intDist));

		showEventNotification(str);
	}

	@Override
	public void routingStateChanged(RoutingService.State state) {
		switch (state) {
		case UPDATING:
			showPanel();
			setPanelUpdatingState(true);
			break;
		case CHOICE:
			showPanel();
			setPanelUpdatingState(false);
			break;
		case IDLE:
			hidePanel();
			setPanelUpdatingState(false);
			break;
		case TRACKING:
			pathsCleared();
			break;

		default:
			hidePanel();
		}
	}

	private void setPanelUpdatingState(boolean isUpdating) {
		View progressBar = view.findViewById(R.id.route_updating_progressbar);
		View button = view.findViewById(R.id.route_button_type);

		progressBar.setVisibility(isUpdating ? View.VISIBLE : View.GONE);
		button.setVisibility(isUpdating ? View.GONE : View.VISIBLE);
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
		assert !paths.isEmpty();

		this.targetPoint = targetPoint;
		this.paths = paths;

		if (paths.get(activeRoutingType) == null) {
			for (RoutingType routingType : RoutingService.REQUIRED_ROUTING_TYPES) {
				if (paths.containsKey(routingType)) {
					changeActiveRoutingType(routingType);
					break;
				}
			}
		}

		updateOverlays();
	}

	@Override
	public void pathsCleared() {
		mapView.getOverlayManager().removeAll(pathOverlays);
		mapView.getOverlayManager().remove(targetPointOverlay);

		mapView.invalidate();

		pathOverlays.clear();
		paths.clear();
		targetPoint = null;
		targetPointOverlay = null;
		activeRoutingType = null;

		hidePanel();
	}

	@Override
	public void activePathUpdated(ChoicePath initialPath, PathPointList pointList) {

	}

	private int getColorByPathType(ChoicePath path) {
		assert path.getRoutingType() != null;

		switch (path.getRoutingType()) {
		default:
		case FASTEST:
			return getResources().getColor(R.color.color_path_danger);
		case NORMAL:
			return getResources().getColor(R.color.color_path_half_safe);
		case SAFE:
			return getResources().getColor(R.color.color_path_safe);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mapView != null) {
			Point clickedPoint = currentListPoints[position];
			mapView.getController().animateTo(clickedPoint.toGeoPoint());
			log.debug("User clicked point {} at {}, {}", clickedPoint.getName(),
					clickedPoint.getLat(), clickedPoint.getLon());
		}
	}

	private class RoutingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			servicesBoundCount++;
			routingService = ((RoutingService.Binder) service).getService();

			if (servicesBoundCount == 2) {
				onServicesReady();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			servicesBoundCount--;
			routingService = null;

			if (servicesBoundCount < 2) {
				onServicesDisconnected();
			}
		}
	}

	private void changeActiveRoutingType(RoutingType routingType) {
		activeRoutingType = routingType;
		appPreferences.setActiveRoutingType(routingType);
	}

	public static class TargetPointItem extends OverlayItem {
		public TargetPointItem(GeoPoint aGeoPoint, Drawable drawable) {
			super("target-point", "target-point", aGeoPoint);
			setMarker(drawable);
		}
	}

	private class PointsServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			servicesBoundCount++;
			pointsService = ((PointsService.Binder) service).getService();

			if (servicesBoundCount == 2) {
				onServicesReady();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			servicesBoundCount--;
			pointsService = null;

			if (servicesBoundCount < 2) {
				onServicesDisconnected();
			}
		}
	}
}
