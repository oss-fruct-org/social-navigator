package org.fruct.oss.socialnavigator.adapters;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Disability;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.utils.Checker;

public class PointAdapter extends CursorAdapter {
	private final Checker checker;

	public PointAdapter(Context context, Checker checker) {
		super(context, null, false);

		this.checker = checker;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		Holder holder = new Holder();
		holder.textView = (CheckedTextView) ((Activity) context).getLayoutInflater().inflate(android.R.layout.simple_list_item_multiple_choice, viewGroup, false);
		holder.textView.setTag(holder);
		return holder.textView;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Disability disability = new Disability(cursor);
		Holder holder = ((Holder) view.getTag());
		holder.textView.setText(disability.getName());
		checker.setChecked(cursor.getPosition(), disability.isActive());
	}

	@Override
	public Cursor getItem(int position) {
		return (Cursor) super.getItem(position);
	}

	private static class Holder {
		CheckedTextView textView;
	}
}
