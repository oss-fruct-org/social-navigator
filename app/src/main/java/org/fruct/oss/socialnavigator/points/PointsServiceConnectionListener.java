package org.fruct.oss.socialnavigator.points;

public interface PointsServiceConnectionListener {
	void onPointsServiceReady(PointsService pointsService);
	void onPointsServiceDisconnected();
}
