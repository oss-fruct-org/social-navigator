package org.fruct.oss.socialnavigator.fragments.root;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.adapters.PointAdapter;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PointFragment extends ListFragment implements PointsService.Listener, SwipeRefreshLayout.OnRefreshListener {
	private static final Logger log = LoggerFactory.getLogger(PointFragment.class);

	private PointAdapter adapter;

	private ServiceConnection pointConnection = new PointConnection();
	private PointsService pointsService;
	private SwipeRefreshLayout refreshLayout;

	public PointFragment() {
	}

	public static PointFragment newInstance() {
		return new PointFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new PointAdapter(getActivity());
		setListAdapter(adapter);

		// Bind service
		Intent intent = new Intent(getActivity(), PointsService.class);
		getActivity().bindService(intent, pointConnection, Context.BIND_AUTO_CREATE);

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		refreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_points, container, false);
		refreshLayout.setOnRefreshListener(this);
		refreshLayout.setColorSchemeColors(0xffff0000, 0xff00ff00, 0xff0000ff, 0xffff00ff);
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

		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.points, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_refresh) {
			onRefresh();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onServiceReady(PointsService service) {
		this.pointsService = service;
		pointsService.addListener(this);
		refreshList();
	}

	@Override
	public void onDataUpdated() {
		refreshLayout.setRefreshing(false);
		refreshList();
	}

	@Override
	public void onDataUpdateFailed(Throwable throwable) {
		refreshLayout.setRefreshing(false);
		Toast.makeText(getActivity(), R.string.str_data_refresh_failed, Toast.LENGTH_SHORT).show();
	}

	private void refreshList() {
		if (pointsService != null) {
			pointsService.queryCursor(pointsService.requestPoints(null), new Function<Cursor>() {
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
		if (pointsService != null) {
			pointsService.refreshProviders();
			refreshLayout.setRefreshing(true);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Point point = new Point(adapter.getItem(position));
		Bundle fragmentArgs = new Bundle();
		fragmentArgs.putParcelable("point", point);

		Intent intent = new Intent(MainActivity.ACTION_SWITCH, null, getActivity(), MainActivity.class);
		intent.putExtra(MainActivity.ARG_INDEX, 0);
		intent.putExtra(MainActivity.ARG_ARGUMENTS, fragmentArgs);
		startActivity(intent);
	}

	private class PointConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			PointsService service = ((PointsService.Binder) binder).getService();
			onServiceReady(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	}
}
