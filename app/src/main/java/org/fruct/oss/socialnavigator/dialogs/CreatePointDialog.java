package org.fruct.oss.socialnavigator.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
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

		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String title = String.valueOf(titleTextView.getText());
				String description = String.valueOf(descriptionTextView.getText());
				String url = String.valueOf(urlTextView.getText());

				Point point = new Point(title, description, url, geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6(), 1, "local", UUID.randomUUID().toString(), 5);
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

	public static interface Listener {
		void pointCreated(Point point);
	}
}
