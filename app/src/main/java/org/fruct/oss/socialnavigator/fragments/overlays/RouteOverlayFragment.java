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
import android.widget.ImageButton;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.PathPointList;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RouteOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private final RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private final PointsServiceConnection pointsServiceConnection = new PointsServiceConnection();

	private PointsService pointsService;
	private RoutingService routingService;

	private int servicesBoundCount;

	private final List<PathOverlay> pathOverlays = new ArrayList<PathOverlay>();
	private ItemizedIconOverlay<TargetPointItem> targetPointOverlay;
	private GeoPoint targetPoint;

	private Map<RoutingType, RoutingService.Path> paths = new EnumMap<RoutingType, RoutingService.Path>(RoutingType.class);
	private RoutingType activeRoutingType = RoutingType.SAFE;

	private MapView mapView;
	private ResourceProxy resourceProxy;
	private View view;

	private Drawable targetPointDrawable;

	private View.OnClickListener typeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			PopupMenu popupMenu = new PopupMenu(getActivity(), v);
			popupMenu.inflate(R.menu.popup_route);
			popupMenu.show();

			popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					RoutingType routingType;
					switch (menuItem.getItemId()) {
					case R.id.action_safe:
						routingType = RoutingType.SAFE;
						break;
					case R.id.action_half_save:
						routingType = RoutingType.NORMAL;
						break;
					case R.id.action_unsafe:
						routingType = RoutingType.FASTEST;
						break;
					default:
						return false;
					}

					pathSelected(routingType);

					return true;
				}
			});
		}
	};

	private View.OnClickListener closeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(RoutingService.ACTION_ROUTE, null, getActivity(), RoutingService.class);
			getActivity().startService(intent);
		}
	};

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		targetPointDrawable = getResources().getDrawable(R.drawable.star);
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

		return view;
	}

	private void showPathInfo(RoutingService.Path path) {
		TextView lengthTextView = (TextView) view.findViewById(R.id.length_text);
		TextView titleTextView = (TextView) view.findViewById(R.id.title_text);

		if (path != null) {
			lengthTextView.setVisibility(View.VISIBLE);
			lengthTextView.setText(Utils.stringDistance(getResources(), path.getDistance()));
			titleTextView.setText(path.getRoutingType().getStringId());

		} else {
			lengthTextView.setText(getResources().getString(R.string.str_path_not_found));
			lengthTextView.setVisibility(View.GONE);
		}
	}

	private void pathSelected(RoutingType routingType) {
		activeRoutingType = routingType;
		routingService.setRoutingTypeActive(routingType);
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

	private void createOverlay(RoutingService.Path path) {
		PathOverlay pathOverlay = new PathOverlay(getColorByPathType(path), 8, resourceProxy);
		pathOverlay.setAlpha(path.getRoutingType() == activeRoutingType ? 255 : 50);
		PathPointList pointList = path.getPointList();

		for (GeoPoint geoPoint : pointList) {
			pathOverlay.addPoint(geoPoint);
		}

		pathOverlays.add(pathOverlay);
		mapView.getOverlayManager().add(pathOverlay);
	}

	private void updateOverlays() {
		mapView.getOverlayManager().removeAll(pathOverlays);
		mapView.getOverlayManager().remove(targetPointOverlay);
		pathOverlays.clear();

		RoutingService.Path currentPath = paths.get(activeRoutingType);
		for (RoutingService.Path path : paths.values()) {
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
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, RoutingService.Path> paths, RoutingType activeType) {
		this.targetPoint = targetPoint;
		this.paths = paths;
		this.activeRoutingType = activeType;

		if (!paths.isEmpty()) {
			showPanel();
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

		hidePanel();
	}

	@Override
	public void activePathUpdated(RoutingService.Path activePath) {

	}

	private int getColorByPathType(RoutingService.Path path) {
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
