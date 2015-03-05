package org.fruct.oss.socialnavigator.utils;

import android.content.res.Resources;
import android.support.annotation.StringRes;

import org.fruct.oss.socialnavigator.R;

import java.util.HashMap;
import java.util.Map;

public class StaticTranslations {
	private final Resources resources;
	private final Map<String, Integer> translationsMap = new HashMap<>();

	public StaticTranslations(Resources resources) {
		this.resources = resources;
	}

	public StaticTranslations addTranslation(String key, @StringRes int valueId) {
		translationsMap.put(key.toLowerCase().trim(), valueId);
		return this;
	}

	public String getString(String key) {
		Integer resId = translationsMap.get(key.toLowerCase().trim());

		if (resId == null)
			return key;

		try {
			return resources.getString(resId);
		} catch (Resources.NotFoundException e) {
			return key;
		}
	}

	public static StaticTranslations createDefault(Resources resources) {
		StaticTranslations trans = new StaticTranslations(resources);

		// Categories
		trans.addTranslation("Rough roads", R.string.tran_rough_road);
		trans.addTranslation("Border", R.string.tran_border);
		trans.addTranslation("Crosswalk", R.string.tran_crosswalk);
		trans.addTranslation("Traffic light", R.string.tran_traffic_light);
		trans.addTranslation("Ramp", R.string.tran_ramp);
		trans.addTranslation("Slope", R.string.tran_slope);
		trans.addTranslation("Stairs", R.string.tran_stairs);

		// Obstacle names
		trans.addTranslation("Crossroad", R.string.tran_crosswalk);
		trans.addTranslation("Rough road", R.string.tran_crosswalk);

		return trans;
	}
}
