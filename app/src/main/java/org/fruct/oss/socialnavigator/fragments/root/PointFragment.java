package org.fruct.oss.socialnavigator.fragments.root;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ListFragment;

import org.fruct.oss.socialnavigator.adapters.PointAdapter;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.Function;

public class PointFragment extends ListFragment implements PointsService.Listener {
	private PointAdapter adapter;

	private ServiceConnection pointConnection = new PointConnection();
	private PointsService pointsService;

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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		adapter.changeCursor(null);

		if (pointsService != null) {
			pointsService.removeListener(this);
			pointsService = null;
		}

		getActivity().unbindService(pointConnection);
	}

	public void onServiceReady(PointsService service) {
		this.pointsService = service;
		pointsService.addListener(this);
		refreshList();
	}

	@Override
	public void onDataUpdated() {
		refreshList();
	}

	private void refreshList() {
		if (pointsService != null) {
			pointsService.queryCursor(pointsService.requestPoints(null), new Function<Cursor>() {
				@Override
				public void call(Cursor cursor) {
					adapter.changeCursor(cursor);
				}
			});
		}
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
