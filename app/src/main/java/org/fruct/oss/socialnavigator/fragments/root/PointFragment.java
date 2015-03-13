package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.adapters.PointAdapter;
import org.fruct.oss.socialnavigator.points.Disability;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.routing.RoutingService;
import org.fruct.oss.socialnavigator.utils.Checker;
import org.fruct.oss.socialnavigator.utils.Function;
import org.osmdroid.util.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME: rename to category fragment
public class PointFragment extends ListFragment implements PointsService.Listener, SwipeRefreshLayout.OnRefreshListener, Checker {
	private static final Logger log = LoggerFactory.getLogger(PointFragment.class);

	private PointAdapter adapter;

	private ServiceConnection pointConnection = new PointConnection();
	private ServiceConnection routingConnection = new RoutingConnection();
	private PointsService pointsService;
	private RoutingService routingService;

	private SwipeRefreshLayout refreshLayout;

	public PointFragment() {
	}

	public static PointFragment newInstance() {
		return new PointFragment();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section2), ActionBar.NAVIGATION_MODE_STANDARD, null);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new PointAdapter(getActivity(), this);
		setListAdapter(adapter);

		// Bind service
		Intent intent = new Intent(getActivity(), PointsService.class);
		getActivity().bindService(intent, pointConnection, Context.BIND_AUTO_CREATE);

		Intent intent2 = new Intent(getActivity(), RoutingService.class);
		getActivity().bindService(intent2, routingConnection, Context.BIND_AUTO_CREATE);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Resources res = getActivity().getResources();
		refreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_points, container, false);
		refreshLayout.setOnRefreshListener(this);
		refreshLayout.setColorSchemeColors(res.getColor(R.color.color_base_1),
				res.getColor(R.color.color_base_2),
				res.getColor(R.color.color_base_3),
				res.getColor(R.color.color_base_4));

		ListView listView = (ListView) refreshLayout.findViewById(android.R.id.list);
		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

		return refreshLayout;
	}

	@Override
	public void onDestroy() {
		adapter.changeCursor(null);
		adapter = null;

		if (pointsService != null) {
			pointsService.removeListener(this);
			pointsService = null;
		}

		getActivity().unbindService(pointConnection);
		getActivity().unbindService(routingConnection);

		super.onDestroy();
	}

	@Override
	public void onStop() {
		super.onStop();

		if (pointsService != null) {
			SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();
			for (int i = 0; i < checkedItemPositions.size(); i++) {
				Disability disability = new Disability(adapter.getItem(i));
				if (disability.isActive() != checkedItemPositions.get(i)) {
					disability.setActive(checkedItemPositions.get(i));
					pointsService.setDisabilityState(disability, disability.isActive());
				}
			}

			pointsService.commitDisabilityStates();
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.categories_fragment_menu, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean isEnabled = checkServices();
		menu.findItem(R.id.action_refresh).setEnabled(isEnabled);
		menu.findItem(R.id.action_open_map).setEnabled(isEnabled);
	}

	private boolean checkServices() {
		return routingService != null && pointsService != null;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			onRefresh();
			return true;
		case R.id.action_open_map:
			((MainActivity) getActivity()).openMapFragment();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onServiceReady(PointsService service) {
		this.pointsService = service;
		pointsService.addListener(this);
		refreshList();
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onDataUpdated() {
		refreshLayout.setRefreshing(false);
		refreshList();
		Toast.makeText(getActivity(), R.string.str_data_refresh_complete, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {
		refreshLayout.setRefreshing(false);
		Toast.makeText(getActivity(), R.string.str_data_refresh_failed, Toast.LENGTH_SHORT).show();
	}

	private void refreshList() {
		if (pointsService != null) {
			pointsService.queryCursor(pointsService.requestDisabilities(), new Function<Cursor>() {
				@Override
				public void call(Cursor cursor) {
					if (adapter != null) {
						adapter.changeCursor(cursor);
					} else {
						cursor.close();
					}
				}
			});
		}
	}

	@Override
	public void onRefresh() {
		if (checkServices()) {
			Location lastLocation = routingService.getLastLocation();
			if (lastLocation != null) {
				pointsService.refresh(new GeoPoint(lastLocation));
				refreshLayout.setRefreshing(true);
			}
		}
	}

	@Override
	public void setChecked(int position, boolean isChecked) {
		getListView().setItemChecked(position, isChecked);
	}

	private class PointConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			PointsService service = ((PointsService.Binder) binder).getService();
			onServiceReady(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			pointsService = null;
		}
	}

	private class RoutingConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			routingService = ((RoutingService.Binder) binder).getService();
			getActivity().supportInvalidateOptionsMenu();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			routingService = null;
		}
	}
}
