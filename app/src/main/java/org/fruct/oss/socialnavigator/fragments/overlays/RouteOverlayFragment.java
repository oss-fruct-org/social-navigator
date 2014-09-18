package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.utils.Utils;
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

					pathSelected(idx);

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

	private MenuItem closeMenuItem;

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
	public void onCreate(Bundle in) {
		super.onCreate(in);

		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map_close_route, menu);
		closeMenuItem = menu.findItem(R.id.action_close_route);
		closeMenuItem.setVisible(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_close_route) {
			if (routingService != null) {
				routingService.newTargetPoint(null);
			}
		}

		return super.onOptionsItemSelected(item);
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

		ImageButton closeButton = (ImageButton) view.findViewById(R.id.route_button_close);
		closeButton.setOnClickListener(closeListener);

		ImageButton typeButton = (ImageButton) view.findViewById(R.id.route_button_type);
		typeButton.setOnClickListener(typeListener);

		return view;
	}

	private void showPathInfo(RoutingService.Path path) {
		TextView lengthTextView = (TextView) view.findViewById(R.id.length_text);
		lengthTextView.setText(Utils.stringDistance(getResources(), path.getResponse().getDistance()));
		mapView.invalidate();
	}

	private void pathSelected(int idx) {
		for (int i = 0; i < pathOverlays.size(); i++) {
			pathOverlays.get(i).setAlpha(i == idx ? 255 : 50);
		}

		RoutingService.Path currentPath = paths.get(idx);
		routingService.setPathActive(currentPath);
		showPathInfo(currentPath);
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

	@Override
	public void pathsUpdated(List<RoutingService.Path> paths) {
		mapView.getOverlayManager().removeAll(pathOverlays);
		pathOverlays.clear();

		for (RoutingService.Path path : paths) {
			PointList pointList = path.getPointList();

			PathOverlay pathOverlay = new PathOverlay(0xff1177ff, 8, resourceProxy);
			pathOverlay.setAlpha(path.isActive() ? 255 : 50);

			for (int i = 0; i < pointList.size(); i++) {
				pathOverlay.addPoint((int) (pointList.getLatitude(i) * 1e6),
						(int) (pointList.getLongitude(i) * 1e6));
			}

			pathOverlays.add(pathOverlay);
			mapView.getOverlayManager().add(pathOverlay);

			if (path.isActive()) {
				showPathInfo(path);
			}
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
		mapView.invalidate();

		pathOverlays.clear();
		paths = Collections.emptyList();
		hidePanel();
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
}
