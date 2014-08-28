package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;

public interface CursorReceiver {
	Cursor swapCursor(Cursor cursor);
}
