package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.List;

public class RouteOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private final RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private final PointsServiceConnection pointsServiceConnection = new PointsServiceConnection();

	private PointsService pointsService;
	private RoutingService routingService;

	private int servicesBoundCount;

	private PathOverlay[] pathOverlays;
	private MapView mapView;

	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);
		getActivity().bindService(new Intent(getActivity(), PointsService.class),
				pointsServiceConnection, Context.BIND_AUTO_CREATE);

		ResourceProxy proxy = new DefaultResourceProxyImpl(getActivity());

		PathOverlay pathOverlay1 = new PathOverlay(0xaa33ff43, 4, proxy);
		PathOverlay pathOverlay2 = new PathOverlay(0xaa3343f4, 4, proxy);
		PathOverlay pathOverlay3 = new PathOverlay(0xaafe3443, 4, proxy);

		pathOverlays = new PathOverlay[] {pathOverlay1, pathOverlay2, pathOverlay3};
		for (PathOverlay pathOverlay : pathOverlays) {
			mapView.getOverlayManager().add(pathOverlay);
		}
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
	public void pathsUpdated(List<RoutingService.RouteResult> paths) {
		int idx = 0;
		for (PathOverlay pathOverlay : pathOverlays) {
			pathOverlay.clearPath();
			PointList pointList = paths.get(idx++).getPointList();

			for (int i = 0; i < pointList.size(); i++) {
				pathOverlay.addPoint((int) (pointList.getLatitude(i) * 1e6),
						(int) (pointList.getLongitude(i) * 1e6));
			}
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
}
