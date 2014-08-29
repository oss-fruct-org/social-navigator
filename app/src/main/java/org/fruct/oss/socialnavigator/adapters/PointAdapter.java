package org.fruct.oss.socialnavigator.adapters;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;

public class PointAdapter extends CursorAdapter {
	public PointAdapter(Context context) {
		super(context, null, false);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		View view = ((Activity) context).getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false);
		Holder holder = new Holder();
		holder.textView = (TextView) view.findViewById(android.R.id.text1);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Point point = new Point(cursor);
		Holder holder = ((Holder) view.getTag());
		holder.textView.setText(point.getName());
	}

	private static class Holder {
		TextView textView;
	}
}
