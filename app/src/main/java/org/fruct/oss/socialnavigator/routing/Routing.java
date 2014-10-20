package org.fruct.oss.socialnavigator.routing;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import org.fruct.oss.socialnavigator.DataService;
import org.fruct.oss.socialnavigator.content.RemoteContentService;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Routing {
	private static final Logger log = LoggerFactory.getLogger(Routing.class);

	private CustomGraphHopper gh;
	private boolean isReady;

	public synchronized void loadFromPref(Context context, String storagePath) {
		if (gh != null) {
			gh.close();
			isReady = false;
		}

		gh = (CustomGraphHopper) new CustomGraphHopper().forMobile();
		gh.disableCHShortcuts();

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

		String ghDirectory = pref.getString(Settings.NAVIGATION_DATA, null);
		if (ghDirectory != null) {
			String ghPath = storagePath + "/graphhopper/" + ghDirectory;
			if (!gh.load(ghPath)) {
				gh.close();
				gh = null;
				throw new RuntimeException("Can't initialize graphhopper in " + ghPath);
			}
			isReady = true;
		}
	}

	public synchronized boolean isReady() {
		return isReady;
	}

	public synchronized void close() {
		if (gh != null) {
			isReady = false;
			gh.close();
			gh = null;
		}
	}

	@Deprecated
	public synchronized List<RoutingService.Path> route(final double fromLat, final double fromLon, final double toLat, final double toLon) {
		return new ArrayList<RoutingService.Path>(3) {
			{
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.SAFE));
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.NORMAL));
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.FASTEST));
			}
		};
	}

	public synchronized RoutingService.Path route(double fromLat, double fromLon, double toLat, double toLon, RoutingType routingType) {
		if (gh == null) {
			return null;
		}

		GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon);
		request.setVehicle(routingType.getVehicle());
		request.setWeighting(routingType.getWeighting());
		GHResponse response = gh.route(request);

		if (!response.isFound()) {
			return null;
		} else {
			return new RoutingService.Path(response, routingType);
		}
	}

	public synchronized void setObstacles(List<Point> points) {
		gh.updateBlockedEdges(points);
	}
}
