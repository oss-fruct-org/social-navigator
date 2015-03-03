package org.fruct.oss.socialnavigator.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.graphhopper.util.PMap;

import org.fruct.oss.socialnavigator.routing.RoutingType;

public class Preferences {
	public static final String PREF_ACTIVE_ROUTING_TYPE = "pref-active-routing-type";
	public static final String PREF_LAST_POINTS_UPDATE_TIMESTAMP = "pref-last-points-update-timestamp";

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
}
