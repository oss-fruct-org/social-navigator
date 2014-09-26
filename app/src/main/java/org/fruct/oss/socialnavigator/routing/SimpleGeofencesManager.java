package org.fruct.oss.socialnavigator.routing;

import android.location.Location;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class SimpleGeofencesManager implements GeofencesManager {
	private List<GeofencesListener> listeners = new CopyOnWriteArrayList<GeofencesListener>();
	private List<List<Geofence>> geofences = new ArrayList<List<Geofence>>();
	private Set<Geofence> geofencesInRange = new HashSet<Geofence>();

	@Override
	public void addListener(GeofencesListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(GeofencesListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public int createToken() {
		for (int i = 0, geofencesSize = geofences.size(); i < geofencesSize; i++) {
			List<Geofence> tokenList = geofences.get(i);
			if (tokenList == null) {
				tokenList = new ArrayList<Geofence>();
				geofences.set(i, tokenList);
				return i;
			}
		}

		geofences.add(new ArrayList<Geofence>());
		return geofences.size() - 1;
	}

	@Override
	public void addGeofence(int token, double lat, double lon, double radius, Bundle data) {
		if (token >= geofences.size() || token < 0 || geofences.get(token) == null) {
			throw new IllegalArgumentException("Wrong geofences token");
		}

		geofences.get(token).add(new Geofence(lat, lon, radius, data));
	}

	@Override
	public boolean removeGeofences(int token) {
		if (token >= geofences.size() || token < 0 || geofences.get(token) == null) {
			return false;
		}

		List<Geofence> geofencesList = geofences.get(token);
		for (Geofence geofence : geofencesList) {
			geofencesInRange.remove(geofence);
		}
		geofencesList.clear();

		return true;
	}

	@Override
	public void setLocation(Location location) {
		ArrayList<Geofence> inRangeList = new ArrayList<Geofence>();
		ArrayList<Geofence> outRangeList = new ArrayList<Geofence>();
		float[] out = new float[1];

		for (List<Geofence> tokenGeofence : geofences) {
			if (tokenGeofence != null) {
				for (Geofence geofence : tokenGeofence) {
					boolean alreadyInRange = geofencesInRange.contains(geofence);
					Location.distanceBetween(location.getLatitude(), location.getLongitude(),
							geofence.lat, geofence.lon, out);

					boolean inRange = out[0] < geofence.rad;

					if (inRange && !alreadyInRange) {
						inRangeList.add(geofence);
					} else if (!inRange && alreadyInRange) {
						outRangeList.add(geofence);
					}
				}
			}
		}

		for (Geofence geofence : outRangeList) {
			for (GeofencesListener listener : listeners) {
				listener.geofenceExited(geofence.data);
			}
			geofencesInRange.remove(geofence);
		}

		for (Geofence geofence : inRangeList) {
			for (GeofencesListener listener : listeners) {
				listener.geofenceEntered(geofence.data);
			}
			geofencesInRange.add(geofence);
		}
	}

	private static class Geofence {
		static int counter = 0;

		double lat;
		double lon;
		double rad;
		Bundle data;
		int idx;

		private Geofence(double lat, double lon, double rad, Bundle data) {
			this.lat = lat;
			this.lon = lon;
			this.rad = rad;
			this.data = data;
			this.idx = counter++;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Geofence geofence = (Geofence) o;

			if (idx != geofence.idx) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return idx;
		}
	}
}
