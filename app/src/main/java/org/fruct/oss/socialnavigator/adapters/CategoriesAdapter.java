package org.fruct.oss.socialnavigator.adapters;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.socialnavigator.fragments.root.GetsFragment;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.PointsService;
import org.fruct.oss.socialnavigator.utils.PublishingTask;
import org.fruct.oss.socialnavigator.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.Holder> {
	private final Category[] categories;
	private final ImageLoader imageLoader;

	private final Map<Category, PublishingTask> publishingTaskMap = new HashMap<>();
	private PointsService pointsService;

	public CategoriesAdapter(Category[] categories) {
		this.categories = categories;
		imageLoader = ImageLoader.getInstance();
	}

	public void close() {
		for (PublishingTask publishingTask : publishingTaskMap.values()) {
			publishingTask.cancel(false);
		}
	}

	public void setPointsService(PointsService pointsService) {
		this.pointsService = pointsService;
	}

	@Override
	public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_category, parent, false);
		return new Holder(view);
	}

	@Override
	public void onBindViewHolder(Holder holder, int position) {
		Category category = categories[position];

		String iconUrl = category.getIconUrl();
		if (!Utils.isNullOrEmpty(iconUrl)) {
			imageLoader.displayImage(iconUrl, holder.imageView);
		}

		holder.textView.setText(category.getDescription());
		holder.category = category;

		holder.imageButton.setOnClickListener(holder);

		PublishingTask publishingTask = publishingTaskMap.get(category);
		setupButton(holder, category, publishingTask != null
				&& publishingTask.getStatus() != AsyncTask.Status.FINISHED);
	}

	@Override
	public int getItemCount() {
		return categories.length;
	}

	private void setupButton(Holder holder, Category category, boolean isUpdating) {
		if (holder.category != category)
			return;

		if (isUpdating) {
			holder.imageButton.setVisibility(View.GONE);
			holder.progressBar.setVisibility(View.VISIBLE);
		} else {
			holder.imageButton.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.GONE);

			holder.imageButton.setColorFilter(category.isPublished() ? 0xff000000 : 0x55ffffff);
		}
	}

	private void startPublishing(final Category category, final Holder holder) {
		final PublishingTask.Mode publishMode = category.isPublished()
				? PublishingTask.Mode.UNPUBLISH : PublishingTask.Mode.PUBLISH;

		PublishingTask publishingTask = new PublishingTask(holder.imageButton.getContext(),
				publishMode, category) {
			@Override
			protected void onPreExecute() {
				setupButton(holder, category, true);
			}

			@Override
			protected void onPostExecute(Result result) {
				if (result == Result.SUCCESS || result == Result.ALREADY) {
					category.setPublished(!category.isPublished());
					if (pointsService != null) {
						pointsService.addCategory(category);
					}
				} else {
					Toast.makeText(pointsService, R.string.no_networks_offline, Toast.LENGTH_SHORT).show();
				}

				setupButton(holder, category, false);
			}
		};

		publishingTaskMap.put(category, publishingTask);
		publishingTask.execute();
	}

	public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
		private final ImageView imageView;
		private final TextView textView;
		private final ImageButton imageButton;
		private final ProgressBar progressBar;

		private Category category;

		public Holder(View itemView) {
			super(itemView);

			imageView = (ImageView) itemView.findViewById(android.R.id.icon);
			textView = (TextView) itemView.findViewById(android.R.id.text1);
			imageButton = (ImageButton) itemView.findViewById(android.R.id.button1);
			progressBar = (ProgressBar) itemView.findViewById(android.R.id.progress);
		}

		@Override
		public void onClick(View v) {
			startPublishing(category, this);
		}
	}

}
