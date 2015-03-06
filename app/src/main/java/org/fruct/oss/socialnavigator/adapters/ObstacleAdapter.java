package org.fruct.oss.socialnavigator.adapters;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.socialnavigator.App;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.utils.StaticTranslations;
import org.fruct.oss.socialnavigator.utils.Utils;

public class ObstacleAdapter extends RecyclerView.Adapter<ObstacleAdapter.Holder> {
	private final StaticTranslations translator;
	private final Point[] points;
	private final ImageLoader imageLoader;

	public ObstacleAdapter(StaticTranslations translator, Point[] points) {
		this.translator = translator;
		this.points = points;
		this.imageLoader = App.getImageLoader();
	}

	@Override
	public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_icon_two_line, viewGroup, false);
		return new Holder(view);
	}

	@Override
	public void onBindViewHolder(Holder holder, int i) {
		Point obstacle = points[i];
		Resources resources = holder.textView2.getResources();
		holder.textView1.setText(translator.getString(obstacle.getName()));
		holder.textView2.setText(resources.getText(R.string.str_difficulty) + ": " + obstacle.getDifficulty());

		String categoryIconUrl = obstacle.getCategory().getIconUrl();
		if (!Utils.isNullOrEmpty(categoryIconUrl)) {
			imageLoader.displayImage(categoryIconUrl, holder.imageView);
		}
	}

	@Override
	public int getItemCount() {
		return points.length;
	}

	public class Holder extends RecyclerView.ViewHolder {
		private TextView textView1;
		private TextView textView2;
		private ImageView imageView;

		public Holder(View view) {
			super(view);

			textView1 = (TextView) view.findViewById(android.R.id.text1);
			textView2 = (TextView) view.findViewById(android.R.id.text2);
			imageView = (ImageView) view.findViewById(android.R.id.icon);
		}
	}
}
