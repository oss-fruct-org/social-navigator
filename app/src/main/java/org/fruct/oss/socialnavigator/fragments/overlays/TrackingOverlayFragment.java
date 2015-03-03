package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.PathPointList;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.Turn;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.Map;

public class TrackingOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private MapView mapView;

	private RoutingService routingService;
	private RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private View view;

	private TextView textView;

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_overlay_tracking, container, false);

		ImageButton closeButton = (ImageButton) view.findViewById(R.id.route_button_close);

		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (routingService != null) {
					routingService.stopTracking();
				}
			}
		});

		textView = (TextView) view.findViewById(R.id.test_text_view);

		return view;
	}


	@Override
	public void onDestroy() {
		getActivity().unbindService(routingServiceConnection);

		if (routingService != null) {
			routingService.removeListener(this);
		}

		super.onDestroy();
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


	@Override
	public void onMapViewReady(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public void proximityEvent(Point point) {
	}

	@Override
	public void proximityEvent(Turn turn) {
	}

	@Override
	public void routingStateChanged(RoutingService.State state) {
		if (state == RoutingService.State.TRACKING) {
			showPanel();
		} else {
			hidePanel();
		}
	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
	}

	@Override
	public void pathsCleared() {
	}

	@Override
	public void activePathUpdated(ChoicePath initialPath, PathPointList pointList) {
		textView.setText("Total obstacles " + initialPath.getPoints().length);
	}

	private class RoutingServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			routingService = ((RoutingService.Binder) service).getService();
			routingService.addListener(TrackingOverlayFragment.this);
			routingService.sendLastResult();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			routingService = null;
		}
	}
}
