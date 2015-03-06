package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;

public interface Request<T> {
	Cursor doQuery();
	T cursorToObject(Cursor cursor);
	int getId(T t);
}
