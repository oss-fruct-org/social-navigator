package org.fruct.oss.socialnavigator.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.graphhopper.util.PMap;

import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.osmdroid.util.GeoPoint;

public class Preferences {
	public static final String PREF_ACTIVE_ROUTING_TYPE = "pref-active-routing-type";
	public static final String PREF_LAST_POINTS_UPDATE_TIMESTAMP = "pref-last-points-update-timestamp";

	public static final String PREF_LAST_POINTS_UPDATE_LAT = "pref-last-points-update-lat";
	public static final String PREF_LAST_POINTS_UPDATE_LON = "pref-last-points-update-lon";

	private SharedPreferences pref;

	public Preferences(Context context) {
		pref = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public Preferences(SharedPreferences sharedPreferences) {
		pref = sharedPreferences;
	}

	public SharedPreferences getPref() {
		return pref;
	}

	public RoutingType getActiveRoutingType() {
		int ord = pref.getInt(PREF_ACTIVE_ROUTING_TYPE, -1);
		if (ord == -1 || RoutingType.values().length <= ord) {
			return RoutingType.SAFE;
		}
		return RoutingType.values()[ord];
	}

	public void setActiveRoutingType(RoutingType routingType) {
		int ord = routingType.ordinal();
		pref.edit().putInt(PREF_ACTIVE_ROUTING_TYPE, ord).apply();
	}

	public void setLastPointsUpdateTimestamp(long currentTime) {
		pref.edit().putLong(PREF_LAST_POINTS_UPDATE_TIMESTAMP, currentTime).apply();
	}

	public long getLastPointsUpdateTimestamp() {
		return pref.getLong(PREF_LAST_POINTS_UPDATE_TIMESTAMP, -1);
	}

	public void setLastPointsUpdateLocation(GeoPoint geoPoint) {
		pref.edit().putInt(PREF_LAST_POINTS_UPDATE_LAT, geoPoint.getLatitudeE6())
				.putInt(PREF_LAST_POINTS_UPDATE_LON, geoPoint.getLongitudeE6()).apply();
	}

	public GeoPoint getLastPointsUpdateLocation() {
		int latE6 = pref.getInt(PREF_LAST_POINTS_UPDATE_LAT, Integer.MAX_VALUE);
		int lonE6 = pref.getInt(PREF_LAST_POINTS_UPDATE_LON, Integer.MAX_VALUE);

		if (latE6 == Integer.MAX_VALUE) {
			return null;
		} else {
			return new GeoPoint(latE6, lonE6);
		}
	}
}
