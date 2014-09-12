package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.MotionEvent;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.List;

public class RouteOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private final RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private final PointsServiceConnection pointsServiceConnection = new PointsServiceConnection();

	private PointsService pointsService;
	private RoutingService routingService;

	private int servicesBoundCount;

	private final List<PathOverlay> pathOverlays = new ArrayList<PathOverlay>();

	private MapView mapView;
	private ResourceProxy resourceProxy;

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

		for (RoutingService.Path path : paths) {
			PointList pointList = path.getPointList();

			PathOverlay pathOverlay = new PathOverlay(0xff1177ff, 8, resourceProxy);


			if (path.isActive()) {
				pathOverlay.setAlpha(255);
			} else {
				pathOverlay.setAlpha(127);
			}

			for (int i = 0; i < pointList.size(); i++) {
				pathOverlay.addPoint((int) (pointList.getLatitude(i) * 1e6),
						(int) (pointList.getLongitude(i) * 1e6));
			}

			mapView.getOverlayManager().add(pathOverlay);
		}

		mapView.invalidate();
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

	private static class ClickablePathOverlay extends PathOverlay {
		public ClickablePathOverlay(int color, float width, ResourceProxy resourceProxy) {
			super(color, width, resourceProxy);
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
			Projection proj = mapView.getProjection();

			//proj.to

			return false;
		}

	}
}
