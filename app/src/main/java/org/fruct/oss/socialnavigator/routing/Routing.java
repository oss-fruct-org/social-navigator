package org.fruct.oss.socialnavigator.routing;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import org.fruct.oss.socialnavigator.points.Point;
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

	private final CustomGraphHopper gh;

	Routing() {
		gh = (CustomGraphHopper) new CustomGraphHopper().forMobile();
		gh.disableCHShortcuts();
	}

	public void load(String directory) {
		gh.load(directory);
	}

	public void loadFromAsset(Context context, String assetFile, int version) throws IOException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		int storedVersion = pref.getInt("pref-routing-stored-version", -1);

		byte[] buffer = new byte[4096];
		File ghPath = new File(context.getCacheDir(), "gh-path");
		File nodesFile = new File(ghPath, "nodes");

		if (!ghPath.mkdirs() && !ghPath.isDirectory()) {
			throw new IOException("Can't create target graphhopper directory " + ghPath);
		}

		if (storedVersion < version || !nodesFile.exists()) {
			AssetManager assets = context.getAssets();
			ZipInputStream zipInputStream = new ZipInputStream(assets.open(assetFile));

			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				File outputFile = new File(ghPath, zipEntry.getName());
				FileOutputStream output = new FileOutputStream(outputFile);

				int read;
				while ((read = zipInputStream.read(buffer)) >= 0) {
					output.write(buffer, 0, read);
				}

				zipInputStream.closeEntry();
			}

			zipInputStream.close();
			pref.edit().putInt("pref-routing-stored-version", version).apply();
			log.info("New routing asset unpacked to {}", ghPath);
		}

		if (!gh.load(ghPath.getPath())) {
			throw new RuntimeException("Can't initialize graphhopper in " + ghPath);
		}
	}

	public void close() {
		gh.close();
	}

	public List<RoutingService.Path> route(final double fromLat, final double fromLon, final double toLat, final double toLon) {
		return new ArrayList<RoutingService.Path>(3) {
			{
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.SAFE));
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.NORMAL));
				add(route(fromLat, fromLon, toLat, toLon, RoutingType.FASTEST));
			}
		};
	}

	public RoutingService.Path route(double fromLat, double fromLon, double toLat, double toLon, RoutingType routingType) {
		GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon);
		request.setVehicle(routingType.getVehicle());
		request.setWeighting(routingType.getWeighting());
		GHResponse response = gh.route(request);
		return new RoutingService.Path(response, routingType);
	}

	public void setObstacles(List<Point> points) {
		gh.updateBlockedEdges(points);
	}
}
