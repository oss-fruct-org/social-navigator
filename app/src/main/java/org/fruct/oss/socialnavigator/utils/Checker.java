package org.fruct.oss.socialnavigator.utils;

/**
 * Allows to set multi-choice item state from adapter
 */
public interface Checker {
	void setChecked(int position, boolean isChecked);
}
