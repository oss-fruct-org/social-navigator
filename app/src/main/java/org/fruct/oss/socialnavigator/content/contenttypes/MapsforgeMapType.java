package org.fruct.oss.socialnavigator.content.contenttypes;

import android.content.Context;
import android.location.Location;

import org.fruct.oss.socialnavigator.content.ContentItem;
import org.fruct.oss.socialnavigator.content.ContentType;
import org.fruct.oss.socialnavigator.content.DirectoryContentItem;
import org.fruct.oss.socialnavigator.content.RemoteContentService;
import org.fruct.oss.socialnavigator.settings.Settings;
import org.fruct.oss.socialnavigator.utils.Region;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.MapReadResult;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public class MapsforgeMapType extends ContentType {
	private static final Logger log = LoggerFactory.getLogger(MapsforgeMapType.class);
	private Map<String, Region> regions;

	public MapsforgeMapType(Context context, Map<String, Region> regions) {
		super(context, RemoteContentService.MAPSFORGE_MAP, "mapsforge-map-current-hash");
		this.regions = regions;
	}

	@Override
	protected void onItemAdded(ContentItem item) {

	}

	@Override
	protected boolean checkLocation(Location location, ContentItem contentItem) {
		boolean ret = false;

		Region region = regions.get(contentItem.getRegionId());
		if (region != null) {
			return region.testHit(location.getLatitude(), location.getLongitude());
		}

		DirectoryContentItem dItem = (DirectoryContentItem) contentItem;
		MapDatabase mapDatabase = new MapDatabase();
		FileOpenResult result = mapDatabase.openFile(new File(dItem.getPath()));
		if (!result.isSuccess()) {
			log.error("Can't read map database {}", result.getErrorMessage());
			return false;
		}

		BoundingBox bbox = mapDatabase.getMapFileInfo().boundingBox;
		if (bbox.contains(new LatLong(location.getLatitude(), location.getLongitude()))) {
			// TODO: add precise detection
			final int zoom = 16;
			Tile tile = new Tile(MercatorProjection.longitudeToTileX(location.getLongitude(), zoom),
					MercatorProjection.latitudeToTileY(location.getLatitude(), zoom), (byte) zoom);
			MapReadResult mapReadResult = mapDatabase.readMapData(tile);
			if (mapReadResult != null)
				ret = true;
		}

		mapDatabase.closeFile();

		return ret;
	}

	@Override
	protected void activateItem(ContentItem item) {
		pref.edit().putString(Settings.OFFLINE_MAP, item.getName()).apply();
	}


	@Override
	protected boolean isCurrentItemActive(ContentItem item) {
		return true;
	}

	@Override
	public void invalidateCurrentContent() {
		currentItem = null;
		pref.edit().remove(Settings.OFFLINE_MAP)
				.remove(configKey)
				.apply();
	}

	@Override
	protected void deactivateCurrentItem() {
		pref.edit().remove(Settings.OFFLINE_MAP)
				.remove(configKey)
				.apply();
		super.deactivateCurrentItem();
	}
}
