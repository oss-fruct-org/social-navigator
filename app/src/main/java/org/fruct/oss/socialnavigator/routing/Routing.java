package org.fruct.oss.socialnavigator.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;

import org.fruct.oss.socialnavigator.points.Point;

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

	public List<RoutingService.Path> route(final double fromLat, final double fromLon, final double toLat, final double toLon) {
		return new ArrayList<RoutingService.Path>(3) {
			{
				add(route(fromLat, fromLon, toLat, toLon, "CAR", "blocking"));
				add(route(fromLat, fromLon, toLat, toLon, "FOOT", "blocking"));
				add(route(fromLat, fromLon, toLat, toLon, "FOOT", "fastest"));
			}
		};
	}

	public RoutingService.Path route(double fromLat, double fromLon, double toLat, double toLon, String vehicle, String weighting) {
		GHRequest request = new GHRequest(fromLat, fromLon, toLat, toLon);
		request.setVehicle(vehicle);
		request.setWeighting(weighting);
		GHResponse response = gh.route(request);
		return new RoutingService.Path(response, vehicle, weighting);
	}

	public void setObstacles(List<Point> points) {
		gh.updateBlockedEdges(points);
	}
}
