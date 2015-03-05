package org.fruct.oss.socialnavigator.adapters;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;

public class ObstacleAdapter extends RecyclerView.Adapter<ObstacleAdapter.Holder> {
	private final Point[] points;

	public ObstacleAdapter(Point[] points) {
		this.points = points;
	}

	@Override
	public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(android.R.layout.simple_list_item_2, viewGroup, false);
		return new Holder(view);
	}

	@Override
	public void onBindViewHolder(Holder holder, int i) {
		Point obstacle = points[i];
		Resources resources = holder.textView2.getResources();
		holder.textView1.setText(obstacle.getName());
		holder.textView2.setText(resources.getText(R.string.str_difficulty) + ": " + obstacle.getDifficulty());
	}

	@Override
	public int getItemCount() {
		return points.length;
	}

	public class Holder extends RecyclerView.ViewHolder {
		private TextView textView1;
		private TextView textView2;

		public Holder(View view) {
			super(view);

			textView1 = (TextView) view.findViewById(android.R.id.text1);
			textView2 = (TextView) view.findViewById(android.R.id.text2);
		}
	}
}