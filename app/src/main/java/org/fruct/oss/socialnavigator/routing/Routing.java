package org.fruct.oss.socialnavigator.routing;

import android.content.Context;
import android.os.Environment;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.util.PointList;

import org.fruct.oss.socialnavigator.points.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Routing {
	private final CustomGraphHopper gh;

	Routing(String directory) {
		gh = (CustomGraphHopper) new CustomGraphHopper().forMobile();
		gh.disableCHShortcuts();
		gh.load(directory);
	}

	public void close() {
		gh.close();
	}

	public List<RoutingService.RouteResult> route(final double fromLat, final double fromLon, final double toLat, final double toLon) {
		return new ArrayList<RoutingService.RouteResult>(3) {
			{
				add(route(fromLat, fromLon, toLat, toLon, "CAR", "blocking"));
				add(route(fromLat, fromLon, toLat, toLon, "FOOT", "blocking"));
				add(route(fromLat, fromLon, toLat, toLon, "FOOT", "fastest"));
			}
		};
	}

	public RoutingService.RouteResult route(double fromLat, double fromLon, double toLat, double toLon, String vehicle, String weighting) {
		GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon);
		request.setVehicle(vehicle);
		request.setWeighting(weighting);

		GHResponse response = gh.route(request);
		return new RoutingService.RouteResult(response.getPoints(), vehicle, weighting);
	}

	public void setObstacles(List<Point> points) {
		gh.updateBlockedEdges(points);
	}
}
