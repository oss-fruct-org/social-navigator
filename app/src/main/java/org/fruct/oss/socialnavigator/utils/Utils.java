package org.fruct.oss.socialnavigator.utils;

import android.content.res.Resources;

import org.fruct.oss.socialnavigator.R;

public class Utils {
	public static String stringDistance(Resources res, double meters) {
		int kmPart = (int) (meters / 1000);
		int meterPart = (int) (meters - kmPart * 1000);

		if (kmPart != 0 && meterPart != 0) {
			return res.getQuantityString(R.plurals.plural_kilometers, kmPart, kmPart)
				+ " " + res.getQuantityString(R.plurals.plural_meters, meterPart, meterPart);
		} else if (meterPart == 0) {
			return res.getQuantityString(R.plurals.plural_kilometers, kmPart, kmPart);
		} else {
			return res.getQuantityString(R.plurals.plural_meters, meterPart, meterPart);
		}
	}
}
