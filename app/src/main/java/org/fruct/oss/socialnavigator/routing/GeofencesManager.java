package org.fruct.oss.socialnavigator.routing;

import android.location.Location;
import android.os.Bundle;

public interface GeofencesManager {
	void addListener(GeofencesListener listener);
	void removeListener(GeofencesListener listener);

	int createToken();
	void addGeofence(int token, double lat, double lon, double radius, Bundle data);
	boolean removeGeofences(int token);

	void setLocation(Location location);

	public static interface GeofencesListener {
		void geofenceEntered(Bundle data);
		void geofenceExited(Bundle data);
	}
}
