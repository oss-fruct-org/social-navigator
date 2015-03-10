package org.fruct.oss.socialnavigator.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.graphhopper.util.PMap;

import org.fruct.oss.socialnavigator.routing.RoutingType;
import org.osmdroid.util.GeoPoint;

public class Preferences {
	public static final String PREF_ACTIVE_ROUTING_TYPE = "pref-active-routing-type";
	public static final String PREF_LAST_POINTS_UPDATE_TIMESTAMP = "pref-last-points-update-timestamp";
	public static final String PREF_GETS_TOKEN = "pref-gets-token";

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

	@Nullable
	public String getGetsToken() {
		return pref.getString(PREF_GETS_TOKEN, null);
	}

	public void setGetsToken(@Nullable String token) {
		pref.edit().putString(PREF_GETS_TOKEN, token).apply();
	}

	public void setGeoPoint(String suffix, @Nullable GeoPoint geoPoint) {
		String key = "pref_geo_point_" + suffix;

		if (geoPoint != null) {
			pref.edit().putInt(key + "_lat", geoPoint.getLatitudeE6())
					.putInt(key + "_lon", geoPoint.getLongitudeE6()).apply();
		} else {
			pref.edit().remove(key + "_lat").remove(key + "_lon").apply();
		}
	}

	public GeoPoint getGeoPoint(String suffix) {
		String key = "pref_geo_point_" + suffix;

		int latE6 = pref.getInt(key + "_lat", Integer.MAX_VALUE);
		int lonE6 = pref.getInt(key + "_lon", Integer.MAX_VALUE);

		if (latE6 == Integer.MAX_VALUE) {
			return null;
		} else {
			return new GeoPoint(latE6, lonE6);
		}
	}

	public <T extends Enum<T> > void setEnum(String suffix, T enumValue) {
		String key = "pref_enum_" + suffix;

		if (enumValue != null) {
			pref.edit().putString(key, enumValue.toString()).apply();
		} else {
			pref.edit().remove(key).apply();
		}
	}

	public <T extends Enum<T>> T getEnum(String suffix, Class<T> enumClass) {
		String key = "pref_enum_" + suffix;

		String enumClassName = pref.getString(key, null);
		if (enumClassName == null) {
			return null;
		} else {
			for (T enumConstant : enumClass.getEnumConstants()) {
				if (enumConstant.name().equals(enumClassName)) {
					return enumConstant;
				}
			}

			return null;
		}
	}
}
