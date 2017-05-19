package org.fruct.oss.socialnavigator.fragments.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.App;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.routing.ChoicePath;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.fruct.oss.socialnavigator.utils.EarthSpace;
import org.fruct.oss.socialnavigator.utils.Space;
import org.fruct.oss.socialnavigator.utils.StaticTranslations;
import org.fruct.oss.socialnavigator.utils.TrackPath;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.PathOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TrackingOverlayFragment extends OverlayFragment implements RoutingService.Listener {
	private static final Logger log = LoggerFactory.getLogger(TrackingOverlayFragment.class);

	public static final int TURN_PROXIMITY_NOTIFICATION = 30;
	public static final int OBJECT_PROXIMITY_NOTIFICATION = 30;
	private MapView mapView;
	private DefaultResourceProxyImpl resourceProxy;

	private RoutingService routingService;
	private RoutingServiceConnection routingServiceConnection = new RoutingServiceConnection();
	private View view;

	private TextView obstacleTextViewTitle;
	private TextView obstacleTextView;
	private TextView obstacleTextViewDist;

	private TextView turnTextViewTitle;
	private TextView turnTextView;

	private ImageView turnImageView;
	private ImageView obstacleImageView;

	private PathOverlay pathOverlay;
	private ChoicePath initialPath;
	private List<Space.Point> pointList;

	private StaticTranslations translator;

	private Space space = new EarthSpace();

	@Override
	public void onCreate(Bundle in) {
		super.onCreate(in);

		getActivity().bindService(new Intent(getActivity(), RoutingService.class),
				routingServiceConnection, Context.BIND_AUTO_CREATE);

		translator = StaticTranslations.createDefault(getResources());
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

		obstacleTextViewTitle = (TextView) view.findViewById(R.id.next_obstacle_title_text_view);
		turnTextViewTitle = (TextView) view.findViewById(R.id.turn_title_text_view);

		obstacleTextView = (TextView) view.findViewById(R.id.next_obstacle_text_view);
		turnTextView = (TextView) view.findViewById(R.id.turn_text_view);

		obstacleTextViewDist = (TextView) view.findViewById(R.id.next_obstacle_dist_text_view);

		turnImageView = (ImageView) view.findViewById(R.id.turn_image_view);
		obstacleImageView = (ImageView) view.findViewById(R.id.obstacle_image_view);


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

		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down_top);

		if (view != null) {
			view.startAnimation(anim);
			view.setVisibility(View.VISIBLE);
		}
	}

	private void hidePanel() {
		if (view.getVisibility() == View.GONE)
			return;

		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up_top);
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
		resourceProxy = new DefaultResourceProxyImpl(getActivity());
        this.getView().setVisibility(View.INVISIBLE);
	}

	@Override
	public void routingStateChanged(RoutingService.State state) {
		if (state == RoutingService.State.TRACKING) {
			showPanel();
		} else {
			hidePanel();
			this.initialPath = null;
			this.pointList = null;
		}

		updateOverlay();
	}

	@Override
	public void progressStateChanged(boolean isActive) {

	}

	@Override
	public void pathsUpdated(GeoPoint targetPoint, Map<RoutingType, ChoicePath> paths) {
	}

	@Override
	public void activePathUpdated(RoutingService.TrackingState trackingState) {
		this.initialPath = trackingState.initialPath;
		TrackPath.Result<Point> lastQueryResult = trackingState.lastQueryResult;
		this.pointList = lastQueryResult.remainingPath;

		// Fill obstacle panel
		if (lastQueryResult.nextPointData != null) {
			obstacleTextViewTitle.setText(R.string.str_next_obstacle);
			obstacleTextView.setText(translator.getString(lastQueryResult.nextPointData.getName()));
			float[] dist = new float[1];
			Location.distanceBetween(
					lastQueryResult.nextPointData.getLat(),
					lastQueryResult.nextPointData.getLon(),
					lastQueryResult.currentPosition.x,
					lastQueryResult.currentPosition.y, dist);
			obstacleTextViewDist.setText(Utils.stringDistance(getResources(), dist[0]));
			String categoryIconUrl = lastQueryResult.nextPointData.getCategory().getIconUrl();
			if (!Utils.isNullOrEmpty(categoryIconUrl)) {
				App.getImageLoader().displayImage(categoryIconUrl, obstacleImageView);
			}

		} else {
			obstacleTextViewTitle.setText(R.string.str_no_obstacles);
			obstacleTextView.setText("");
			obstacleTextViewDist.setText("");
			obstacleImageView.setImageResource(android.R.color.transparent);
		}

		// Fill turn panel
		boolean isTurnShown = false;
		if (lastQueryResult.nextTurn != null
				&& lastQueryResult.nextTurn.getTurnSharpness() > 1) {
			double dist = space.dist(lastQueryResult.nextTurn.getPoint(),
					lastQueryResult.currentPosition);

			if (dist < TURN_PROXIMITY_NOTIFICATION) {
				isTurnShown = true;
				turnTextViewTitle.setText(R.string.str_turn_in);
				turnTextView.setVisibility(View.VISIBLE);
				turnTextView.setText(Utils.stringDistance(getResources(), dist));
				turnImageView.setImageResource(lastQueryResult.nextTurn.getTurnDirection() > 0
						? R.drawable.ic_left_arrow
						: R.drawable.ic_right_arrow);
			}
		}

		if (!isTurnShown) {
			turnTextViewTitle.setText(R.string.str_turn_forward);
			turnTextView.setVisibility(View.GONE);
			turnImageView.setImageResource(R.drawable.ic_forward_arrow);
		}

		log.debug("Remaining dist {}", lastQueryResult.remainingDist);

		updateOverlay();
	}

	private void updateOverlay() {
		
		mapView.getOverlayManager().remove(pathOverlay);

		if (initialPath == null || pointList == null) {
			return;
		}

		pathOverlay = new PathOverlay(Utils.getColorByPathType(getResources(), initialPath),
				8, resourceProxy);

		for (Space.Point point : pointList) {
			pathOverlay.addPoint(new GeoPoint(point.x, point.y));
		}

		mapView.getOverlayManager().add(pathOverlay);
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
