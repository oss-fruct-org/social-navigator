package org.fruct.oss.socialnavigator.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.osmdroid.util.GeoPoint;

import java.util.UUID;

public class CreatePointDialog extends DialogFragment {
	private Listener listener;
	private GeoPoint geoPoint;

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
		spinner.setAdapter(createSpinnerAdapter());


		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String title = String.valueOf(titleTextView.getText());
				String description = String.valueOf(descriptionTextView.getText());
				String url = String.valueOf(urlTextView.getText());
				int diff = Integer.parseInt(((String) spinner.getSelectedItem()));

				Point point = new Point(title, description, url, geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6(), 1, "local", UUID.randomUUID().toString(), diff);
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

	private SpinnerAdapter createSpinnerAdapter() {
		String[] arr = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, arr);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return arrayAdapter;
	}

	public static interface Listener {
		void pointCreated(Point point);
	}
}
