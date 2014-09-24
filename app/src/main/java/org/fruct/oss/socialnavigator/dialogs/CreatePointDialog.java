package org.fruct.oss.socialnavigator.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.UUID;

public class CreatePointDialog extends DialogFragment {
	private Listener listener;
	private GeoPoint geoPoint;

	private PointServiceConnection pointServiceConnection = new PointServiceConnection();
	private ArrayAdapter<String> categoryAdapter;
	private List<Category> categories;

	public static CreatePointDialog newInstance(GeoPoint geoPoint) {
		CreatePointDialog dialog = new CreatePointDialog();

		Bundle args = new Bundle();
		args.putParcelable("geoPoint", geoPoint);
		dialog.setArguments(args);

		return dialog;
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActivity().bindService(new Intent(getActivity(), PointsService.class), pointServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onDestroy() {
		getActivity().unbindService(pointServiceConnection);

		super.onDestroy();
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		geoPoint = getArguments().getParcelable("geoPoint");

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();

		View view = inflater.inflate(R.layout.dialog_add_point, null);

		final TextView titleTextView = (TextView) view.findViewById(R.id.text_title);
		final TextView descriptionTextView = (TextView) view.findViewById(R.id.text_description);
		final TextView urlTextView = (TextView) view.findViewById(R.id.text_url);

		final Spinner spinner = (Spinner) view.findViewById(R.id.spinner_difficulty);
		spinner.setAdapter(createDifficultySpinnerAdapter());

		final Spinner categorySpinner = (Spinner) view.findViewById(R.id.spinner_category);
		categorySpinner.setAdapter(createCategorySpinnerAdapter());

		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String title = String.valueOf(titleTextView.getText());
				String description = String.valueOf(descriptionTextView.getText());
				String url = String.valueOf(urlTextView.getText());
				int difficulty = Integer.parseInt(((String) spinner.getSelectedItem()));

				Category category = categories.get(categorySpinner.getSelectedItemPosition());

				Point point = new Point(title, description, url, geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6(), category.getId(), "local", UUID.randomUUID().toString(), difficulty);
				if (listener != null) {
					listener.pointCreated(point);
				}
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		return builder.create();
	}

	private SpinnerAdapter createDifficultySpinnerAdapter() {
		String[] arr = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, arr);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return arrayAdapter;
	}

	private SpinnerAdapter createCategorySpinnerAdapter() {
		if (categoryAdapter== null) {
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
			arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			categoryAdapter = arrayAdapter;
		}

		return categoryAdapter;
	}

	public static interface Listener {
		void pointCreated(Point point);
	}

	private class PointServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			PointsService pointsService = ((PointsService.Binder) service).getService();
			categories = pointsService.queryList(pointsService.requestCategories());

			for (Category category : categories) {
				categoryAdapter.add(category.getDescription());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	}
}
