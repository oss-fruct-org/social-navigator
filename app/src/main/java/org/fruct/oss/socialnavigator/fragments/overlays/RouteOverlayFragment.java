package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;

import com.graphhopper.routing.Path;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private final RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private final PointsServiceConnection pointsServiceConnection = new PointsServiceConnection();

	private PointsService pointsService;
	private RoutingService routingService;

	private int servicesBoundCount;

	private final List<PathOverlay> pathOverlays = new ArrayList<PathOverlay>();

	private List<RoutingService.Path> paths = Collections.emptyList();

	private MapView mapView;
	private ResourceProxy resourceProxy;
	private View view;

	private View.OnClickListener typeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			PopupMenu popupMenu = new PopupMenu(getActivity(), v);
			popupMenu.inflate(R.menu.popup_route);
			popupMenu.show();

			popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem menuItem) {
					int idx = 0;
					switch (menuItem.getItemId()) {
					case R.id.action_safe:
						idx = 0;
						break;
					case R.id.action_half_save:
						idx = 1;
						break;
					case R.id.action_unsafe:
						idx = 2;
						break;
					default:
						return false;
					}

					for (int i = 0; i < pathOverlays.size(); i++) {
						pathOverlays.get(i).setAlpha(i == idx ? 255 : 50);
					}

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
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection, Context.BIND_AUTO_CREATE);

		resourceProxy = new DefaultResourceProxyImpl(getActivity());
	}

	@Override
	public void onDestroy() {
		getActivity().unbindService(routingServiceConnection);
		getActivity().unbindService(pointsServiceConnection);
		routingService.removeListener(this);

		super.onDestroy();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_overlay_route, container, false);

		ImageButton typeButton = (ImageButton) view.findViewById(R.id.route_button);
		typeButton.setOnClickListener(typeListener);

		ImageButton closeButton = (ImageButton) view.findViewById(R.id.route_button_close);
		closeButton.setOnClickListener(closeListener);

		return view;
	}

	private void showPanel() {
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);

		if (view != null) {
			view.startAnimation(anim);
			view.setVisibility(View.VISIBLE);
		}
	}

	private void hidePanel() {
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

	@Override
	public void pathsUpdated(List<RoutingService.Path> paths) {
		mapView.getOverlayManager().removeAll(pathOverlays);
		pathOverlays.clear();

		for (RoutingService.Path path : paths) {
			PointList pointList = path.getPointList();

			PathOverlay pathOverlay = new PathOverlay(0xff1177ff, 8, resourceProxy);

			//if (path.isActive()) {
				pathOverlay.setAlpha(255);
			//} else {
			//	pathOverlay.setAlpha(50);
			//}

			for (int i = 0; i < pointList.size(); i++) {
				pathOverlay.addPoint((int) (pointList.getLatitude(i) * 1e6),
						(int) (pointList.getLongitude(i) * 1e6));
			}

			pathOverlays.add(pathOverlay);
			mapView.getOverlayManager().add(pathOverlay);
		}

		mapView.invalidate();

		this.paths = paths;

		if (!paths.isEmpty()) {
			showPanel();
		}
	}

	@Override
	public void pathsCleared() {
		mapView.getOverlayManager().removeAll(pathOverlays);
		pathOverlays.clear();
		paths = Collections.emptyList();
		hidePanel();
	}

	private void onPathClicked(RoutingService.Path path) {
		if (routingService != null) {
			routingService.setPathActive(path);
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

	private class ClickablePathOverlay extends PathOverlay {
		public static final float MAX_DIST = 20;

		private List<Point> points = new ArrayList<Point>();
		private Point tmpPoint = new Point();
		private GeoPoint tmpGeoPoint = new GeoPoint(0, 0);

		private boolean isPrecomputed;
		private RoutingService.Path path;

		public ClickablePathOverlay(int color, float width, ResourceProxy resourceProxy, RoutingService.Path path) {
			super(color, width, resourceProxy);
			this.path = path;
		}

		public void addPoint(int latE6, int lonE6) {
			super.addPoint(latE6, lonE6);
			points.add(new Point(latE6, lonE6));
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
			Projection proj = mapView.getProjection();

			if (!isPrecomputed) {
				for (Point point : points) {
					proj.toProjectedPixels(point.x, point.y, point);
				}
				isPrecomputed = true;
			}

			float nearest = MAX_DIST * MAX_DIST;
			boolean found = false;

			for (Point point : points) {
				proj.toPixelsFromProjected(point, tmpPoint);

				float dx = tmpPoint.x - e.getX();
				float dy = tmpPoint.y - e.getY();

				float dist = dx * dx + dy * dy;
				if (dist < nearest) {
					nearest = dist;
					found = true;
				}
			}

			if (found) {
				onPathClicked(path);
				return true;
			}

			return false;
		}

	}

}
