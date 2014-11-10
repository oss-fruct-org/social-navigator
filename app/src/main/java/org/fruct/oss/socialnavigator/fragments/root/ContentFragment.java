package org.fruct.oss.socialnavigator.fragments.root;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.fruct.oss.socialnavigator.MainActivity;
import org.fruct.oss.socialnavigator.R;
import org.fruct.oss.mapcontent.content.ContentItem;
import org.fruct.oss.mapcontent.content.RemoteContentService;
import org.fruct.oss.socialnavigator.fragments.other.DownloadProgressFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ContentListItem implements Comparable<ContentListItem> {
	List<ContentListSubItem> contentSubItems;
	String name;

	@Override
	public int compareTo(ContentListItem another) {
		return name.compareTo(another.name);
	}

}

class ContentListSubItem implements Comparable<ContentListSubItem> {
	ContentListSubItem(ContentItem contentItem, ContentFragment.LocalContentState state) {
		this.contentItem = contentItem;
		this.state = state;
	}

	ContentItem contentItem;
	ContentFragment.LocalContentState state;
	Object tag;

	@Override
	public int compareTo(ContentListSubItem another) {
		return contentItem.getType().compareTo(another.contentItem.getType());
	}
}

class ContentAdapter extends BaseAdapter {
	private List<ContentListItem> items;
	private final Context context;
	private final int resource;

	public ContentAdapter(Context context, int resource, List<ContentListItem> objects) {
		this.resource = resource;
		this.items = objects;
		this.context = context;
	}

	public void setItems(List<ContentListItem> items) {
		this.items = items;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public ContentListItem getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).name.hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ContentListItem item = getItem(position);
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		View view = null;
		Holder tag = null;

		if (convertView != null && convertView.getTag() != null) {
			tag = (Holder) convertView.getTag();
			if (tag instanceof Holder) {
				view = convertView;
			}
		}

		if (view == null) {
			view = inflater.inflate(resource, parent, false);
			assert view != null;

			tag = new Holder();
			tag.text1 = (TextView) view.findViewById(android.R.id.text1);
			tag.text2 = (TextView) view.findViewById(android.R.id.text2);
			tag.text3 = (TextView) view.findViewById(R.id.text3);
			view.setTag(tag);
			tag.parent = view;
		}

		tag.text1.setText(item.name);

		int idx = 0;
		TextView[] views = {tag.text2, tag.text3};
		for (TextView view2 : views)
			view2.setVisibility(View.GONE);

		for (ContentListSubItem subItem : item.contentSubItems) {
			subItem.tag = tag;
			ContentFragment.LocalContentState state = subItem.state;
			ContentItem sItem = subItem.contentItem;

			boolean active = false;
			boolean needUpdate = false;

			if (state == ContentFragment.LocalContentState.NOT_EXISTS) {
				active = true;
			} else if (state == ContentFragment.LocalContentState.NEEDS_UPDATE) {
				active = true;
				needUpdate = true;
			}

			if (idx >= views.length)
				break;

			if (idx == 0)
				tag.item1 = sItem;
			else
				tag.item2 = sItem;

			String text = "";
			if (sItem.getType().equals(RemoteContentService.GRAPHHOPPER_MAP)) {
				text = context.getString(R.string.navigation_data);
			} else if (sItem.getType().equals(RemoteContentService.MAPSFORGE_MAP)) {
				text = context.getString(R.string.offline_map);
			}

			if (needUpdate)
				text += " (" + context.getString(R.string.update_availabe) + ")";
			views[idx].setText(text);

			views[idx].setVisibility(View.VISIBLE);
			if (active)
				views[idx].setTypeface(null, Typeface.BOLD);
			else
				views[idx].setTypeface(null, Typeface.NORMAL);

			idx++;
		}

		return view;
	}

	class Holder {
		View parent;
		TextView text1;
		TextView text2;
		TextView text3;

		// Item corresponding second text line
		ContentItem item1;

		// Item corresponding third text line
		ContentItem item2;
	}
}

public class ContentFragment extends Fragment
		implements AdapterView.OnItemClickListener, RemoteContentService.Listener,
		ContentDialog.Listener, ActionMode.Callback, DownloadProgressFragment.OnFragmentInteractionListener,
		ActionBar.OnNavigationListener{
	private final static Logger log = LoggerFactory.getLogger(ContentFragment.class);

	public static ContentFragment newInstance() {
        ContentFragment fragment = new ContentFragment();
        return fragment;
    }
    public ContentFragment() {
        // Required empty public constructor
    }

	private RemoteContentConnection remoteContentConnection = new RemoteContentConnection();

	private ListView listView;
	private ContentAdapter adapter;

	private MenuItem downloadItem;
	private MenuItem useItem;

	// Last selected item
	private ContentListItem currentItem;
	private String currentItemName;

	private LocalContentState filteredState;

	private RemoteContentService remoteContent;

	private List<ContentItem> localItems = Collections.emptyList();
	private List<ContentItem> remoteItems = Collections.emptyList();

	private SharedPreferences pref;

	private DownloadProgressFragment downloadFragment;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		((MainActivity) activity).onSectionAttached(activity.getString(R.string.title_section3), ActionBar.NAVIGATION_MODE_LIST);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_content, container, false);

		if (savedInstanceState != null)
			currentItemName = savedInstanceState.getString("current-item-idx");

		setupSpinner();

		adapter = new ContentAdapter(getActivity(), R.layout.file_list_item, Collections.<ContentListItem>emptyList());

		listView = (ListView) view.findViewById(R.id.list);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

		listView.setAdapter(adapter);

		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

		return view;
	}


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		downloadFragment = (DownloadProgressFragment) getActivity().getSupportFragmentManager().findFragmentByTag("download-fragment");
		if (downloadFragment == null) {
			downloadFragment = new DownloadProgressFragment();
			getActivity().getSupportFragmentManager().beginTransaction()
					.add(R.id.fragment, downloadFragment, "download-fragment")
					.hide(downloadFragment)
					.addToBackStack(null)
					.commit();
		}

		ContentDialog contentDialog = (ContentDialog) getActivity().getSupportFragmentManager().findFragmentByTag("content-dialog");
		if (contentDialog != null) {
			contentDialog.setListener(this);
		}

		downloadFragment.setListener(this);
		getActivity().bindService(new Intent(getActivity(), RemoteContentService.class), remoteContentConnection, Context.BIND_AUTO_CREATE);
		setHasOptionsMenu(true);
	}

	private void setupSpinner() {
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.content_spinner,
				android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(spinnerAdapter, this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("current-item-idx", currentItemName);
	}

	@Override
	public void onDestroy() {
		if (remoteContent != null) {
			remoteContent.removeListener(this);
			remoteContent = null;
		}

		getActivity().unbindService(remoteContentConnection);

		super.onDestroy();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		getActivity().getMenuInflater().inflate(R.menu.refresh, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_refresh:
			if (remoteContent != null) {
				remoteContent.refresh();
			}
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	public void remoteContentServiceReady(RemoteContentService service) {
		remoteContent = service;
		remoteContent.addListener(this);

		setContentList(localItems = new ArrayList<ContentItem>(remoteContent.getLocalItems()),
				remoteItems = new ArrayList<ContentItem>(remoteContent.getRemoteItems()));
	}


	private void setContentList(final List<ContentItem> localItems, final List<ContentItem> remoteItems) {
		new GenerateContentList(localItems, remoteItems, filteredState).execute();
	}

	private void showToast(final String string) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getActivity(), string, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void localListReady(List<ContentItem> list) {
		localItems = new ArrayList<ContentItem>(list);
		setContentList(localItems, remoteItems);
	}

	@Override
	public void remoteListReady(List<ContentItem> list) {
		remoteItems = new ArrayList<ContentItem>(list);
		setContentList(localItems, remoteItems);
	}

	@Override
	public void downloadStateUpdated(ContentItem item, int downloaded, int max) {
		downloadFragment.downloadStateUpdated(item, downloaded, max);
	}

	@Override
	public void downloadFinished(ContentItem localItem, ContentItem remoteItem) {
		showToast(getString(R.string.download_finished));
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				downloadFragment.stopDownload();
			}
		});
	}

	@Override
	public void errorDownloading(ContentItem item, IOException e) {
		showToast(getString(R.string.error_downloading));
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				downloadFragment.stopDownload();
			}
		});
	}

	@Override
	public void errorInitializing(IOException e) {
		showToast(getString(R.string.no_networks_offline));
	}

	@Override
	public void downloadInterrupted(ContentItem sItem) {
		showToast(getString(R.string.download_interrupted));
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				downloadFragment.stopDownload();
				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void downloadsSelected(List<ContentListSubItem> items) {
		if (currentItem == null)
			return;

		downloadFragment.startDownload();
		for (ContentListSubItem item : items)
			remoteContent.downloadItem(item.contentItem);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		currentItem = adapter.getItem(position);
		((ActionBarActivity) getActivity()).startSupportActionMode(this);
	}

	@Override
	public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
		actionMode.getMenuInflater().inflate(R.menu.online_content_action, menu);

		boolean hasDeletable = false;
		boolean hasDownloadable = false;

		for (ContentListSubItem contentSubItem : currentItem.contentSubItems) {
			if (contentSubItem.contentItem.isDownloadable() || contentSubItem.state == LocalContentState.NEEDS_UPDATE) {
				hasDownloadable = true;
			}

			if (!contentSubItem.contentItem.isReadonly()) {
				hasDeletable = true;
			}
		}

		if (!hasDeletable) {
			menu.findItem(R.id.action_delete).setVisible(false);
		}

		if (!hasDownloadable) {
			menu.findItem(R.id.action_download).setVisible(false);
		}

		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
		if (menuItem.getItemId() == R.id.action_download) {
			final ContentDialog dialog = new ContentDialog();
			dialog.setListener(this);
			dialog.setStorageItems(currentItem.contentSubItems);
			dialog.show(getFragmentManager(), "content-dialog");
		} else if (menuItem.getItemId() == R.id.action_delete) {
			deleteContentItem(currentItem);
		} else if (menuItem.getItemId() == R.id.action_use) {
			useContentItem(currentItem);
		} else {
			return false;
		}

		actionMode.finish();

		return false;
	}

	private void deleteContentItem(ContentListItem currentItem) {
		boolean wasError = false;
		if (remoteContent != null && !currentItem.contentSubItems.isEmpty()) {
			for (ContentListSubItem subItem : currentItem.contentSubItems) {
				if (!remoteContent.deleteContentItem(subItem.contentItem))
					wasError = true;
			}
		}

		if (wasError) {
			Toast.makeText(getActivity(), getString(R.string.str_cant_delete_active_content), Toast.LENGTH_LONG).show();
		}
	}

	private void useContentItem(ContentListItem currentItem) {
		throw new IllegalStateException("Not implemented yet");
		//if (remoteContent != null && !currentItem.contentSubItems.isEmpty()) {
		//	remoteContent.activateRegionById(currentItem.contentSubItems.get(0).contentItem.getRegionId());
		//}
	}

	@Override
	public void stopButtonPressed() {
		if (remoteContent != null) {
			remoteContent.interrupt();
		}
	}

	@Override
	public void onDestroyActionMode(ActionMode actionMode) {
		listView.clearChoices();
		listView.setItemChecked(-1, true);
	}

	@Override
	public boolean onNavigationItemSelected(int i, long l) {
		switch (i) {
		case 0: // All
			new GenerateContentList(localItems, remoteItems, filteredState = null).execute();
			return true;

		case 1: // Local
			new GenerateContentList(localItems, remoteItems, filteredState = LocalContentState.UP_TO_DATE).execute();
			return true;

		case 2: // Updates
			new GenerateContentList(localItems, remoteItems, filteredState = LocalContentState.NEEDS_UPDATE).execute();
			return true;
		}

		return false;
	}

	private class GenerateContentList extends AsyncTask<Void, Void, List<ContentListItem>> {
		private final LocalContentState filterState;
		private List<ContentItem> localItems;
		private List<ContentItem> remoteItems;

		private GenerateContentList(List<ContentItem> localItems, List<ContentItem> remoteItems, LocalContentState filterState) {
			this.localItems = localItems;
			this.remoteItems = remoteItems;
			this.filterState = filterState;
		}

		@Override
		protected List<ContentListItem> doInBackground(Void... params) {
			HashMap<String, ContentListSubItem> states
					= new HashMap<String, ContentListSubItem>(localItems.size());

			for (ContentItem item : localItems) {
				states.put(item.getName(), new ContentListSubItem(item, LocalContentState.DELETED_FROM_SERVER));
			}

			for (ContentItem remoteItem : remoteItems) {
				String name = remoteItem.getName();

				ContentListSubItem subItem = states.get(name);
				ContentItem localItem = subItem == null ? null : subItem.contentItem;

				LocalContentState newState;
				ContentItem saveItem = remoteItem;

				if (localItem == null) {
					newState = LocalContentState.NOT_EXISTS;
				} else if (!localItem.getHash().equals(remoteItem.getHash())) {
					newState = LocalContentState.NEEDS_UPDATE;
				} else {
					saveItem = localItem;
					newState = LocalContentState.UP_TO_DATE;
				}

				states.put(name, new ContentListSubItem(saveItem, newState));
			}

			HashMap<String, List<ContentListSubItem>> listViewMap
					= new HashMap<String, List<ContentListSubItem>>();

			for (Map.Entry<String, ContentListSubItem> entry : states.entrySet()) {
				String rId = entry.getValue().contentItem.getRegionId();

				if (filterState != null
						&& filterState != entry.getValue().state
						&& ((filterState != LocalContentState.UP_TO_DATE
						|| entry.getValue().state != LocalContentState.NEEDS_UPDATE))) {
					continue;
				}

				List<ContentListSubItem> l = listViewMap.get(rId);

				if (l == null) {
					l = new ArrayList<ContentListSubItem>();
					listViewMap.put(rId, l);
				}

				l.add(entry.getValue());
			}

			List<ContentListItem> listViewItems = new ArrayList<ContentListItem>();
			for (Map.Entry<String, List<ContentListSubItem>> entry : listViewMap.entrySet()) {
				ContentListItem listViewItem = new ContentListItem();

				listViewItem.name = entry.getValue().get(0).contentItem.getDescription();
				listViewItem.contentSubItems = entry.getValue();

				Collections.sort(listViewItem.contentSubItems);
				listViewItems.add(listViewItem);
			}

			Collections.sort(listViewItems);

			return listViewItems;
		}

		@Override
		protected void onPostExecute(List<ContentListItem> contentListItems) {
			if (contentListItems != null && adapter != null) {
				adapter.setItems(contentListItems);
			}
		}
	}

	private class RemoteContentConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			RemoteContentService service = ((RemoteContentService.Binder) binder).getService();
			remoteContentServiceReady(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	}

	public enum LocalContentState {
		NOT_EXISTS, NEEDS_UPDATE, UP_TO_DATE, DELETED_FROM_SERVER
	}

}
