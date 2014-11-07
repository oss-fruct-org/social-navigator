package org.fruct.oss.socialnavigator.routing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

	@Nullable
	public synchronized RoutingService.Path route(double fromLat, double fromLon, double toLat, double toLon, RoutingType routingType) {
		if (gh == null) {
			return null;
		}

		GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon);
		request.setVehicle(routingType.getVehicle());
		request.setWeighting(routingType.getWeighting());

		try {
			return gh.routePath(request, routingType);
		} catch (Exception ex) {
			log.error("Routing error for routing type: {}", routingType, ex);
			return null;
		}
	}

	public synchronized void setObstacles(List<Point> points) {
		gh.updateBlockedEdges(points);
	}
}
