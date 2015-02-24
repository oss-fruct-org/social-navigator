package org.fruct.oss.socialnavigator.routing;

import org.fruct.oss.socialnavigator.R;

public enum RoutingType {
	SAFE("blocking", "pfoot", R.string.action_route_safe),
	NORMAL("half-blocking", "pfoot", R.string.action_route_half_safe),
	FASTEST("fastest", "pfoot", R.string.action_route_unsafe);

	private final int stringId;
	private final String weighting;
	private final String vehicle;

	private RoutingType(String weighting, String vehicle, int stringId) {
		this.weighting = weighting;
		this.vehicle = vehicle;
		this.stringId = stringId;
	}

	public String getWeighting() {
		return weighting;
	}

	public String getVehicle() {
		return vehicle;
	}

	public int getStringId() {
		return stringId;
	}
}
