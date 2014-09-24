package org.fruct.oss.socialnavigator.routing;

public enum RoutingType {
	FASTEST("fastest", "foot"), NORMAL("half-blocking", "pfoot"), SAFE("blocking", "pfoot");

	private String weighting;
	private String vehicle;

	private RoutingType(String weighting, String vehicle) {

		this.weighting = weighting;
		this.vehicle = vehicle;
	}

	public String getWeighting() {
		return weighting;
	}

	public String getVehicle() {
		return vehicle;
	}
}
