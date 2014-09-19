package org.fruct.oss.socialnavigator.points;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.io.IOException;

public class PointsDatabase implements Closeable {
	public static final int VERSION = 2;
	private final Context context;
	private final Helper helper;
	private final SQLiteDatabase db;

	private static final String[] COLUMNS_ID = { "_id" };
	private static final String[] COLUMNS_CATEGORY = { "_id", "name", "description", "url"};
	private static final String[] COLUMNS_POINT = { "_id", "name", "description", "url", "lat", "lon", "categoryId", "provider", "uuid", "difficulty" };

	public PointsDatabase(Context context) {
		this.context = context;
		this.helper = new Helper(context);
		this.db = helper.getWritableDatabase();
	}

	@Override
	public void close() {
		helper.close();
	}

	public void insertCategory(Category category) {
		ContentValues cv = new ContentValues(4);
		cv.put("name", category.getName());
		cv.put("description", category.getDescription());
		cv.put("url", category.getUrl());

		if (!isCategoryExists(category)) {
			cv.put("_id", category.getId());
			db.insert("category", null, cv);
		} else {
			db.update("category", cv, "_id=?", toArray(category.getId()));
		}
	}

	public void insertPoint(Point point) {
		ContentValues cv = new ContentValues(8);
		cv.put("name", point.getName());
		cv.put("description", point.getDescription());
		cv.put("url", point.getUrl());
		cv.put("lat", point.getLatE6());
		cv.put("lon", point.getLonE6());
		cv.put("categoryId", point.getCategoryId());
		cv.put("provider", point.getProvider());
		cv.put("difficulty", point.getDifficulty());

		if (!isPointExists(point)) {
			cv.put("uuid", point.getUuid());
			db.insert("point", null, cv);
		} else {
			db.update("point", cv, "uuid=?", toArray(point.getUuid()));
		}
	}

	public Cursor loadCategories() {
		return db.query("category", COLUMNS_CATEGORY, null, null, null, null, "name");
	}

	public Cursor loadPoints(Category category) {
		if (category != null)
			return loadPoints(category.getId());
		else
			return db.query("point", COLUMNS_POINT, null, null, null, null, null);
	}

	public Cursor loadPoints(int categoryId) {
		return db.query("point", COLUMNS_POINT, "categoryId=?", toArray(categoryId), null, null, null);
	}

	private boolean isCategoryExists(Category category) {
		Cursor cursor = db.query("category", COLUMNS_ID, "_id=?", toArray(category.getId()), null, null, null);
		boolean isExists = cursor.moveToFirst();
		cursor.close();
		return isExists;
	}

	private boolean isPointExists(Point point) {
		Cursor cursor = db.query("point", COLUMNS_ID, "uuid=?", toArray(point.getUuid()), null, null, null);
		boolean isExists = cursor.moveToFirst();
		cursor.close();
		return isExists;
	}

	private static String[] toArray(Object... objects) {
		String[] arr = new String[objects.length];
		int c = 0;
		for (Object obj : objects) {
			arr[c++] = obj.toString();
		}
		return arr;
	}

	private static class Helper extends SQLiteOpenHelper {
		public Helper(Context context) {
			super(context, "points-db", null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE category " +
					"(_id INTEGER PRIMARY KEY," +
					"name TEXT," +
					"description TEXT," +
					"url TEXT);");

			db.execSQL("CREATE TABLE point " +
					"(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"name TEXT," +
					"description TEXT," +
					"url TEXT," +
					"lat INTEGER," +
					"lon INTEGER," +
					"categoryId," +
					"provider TEXT," +
					"uuid TEXT," +
					"difficulty INTEGER," +
					"FOREIGN KEY(categoryId) REFERENCES category(_id));");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE point;");
			db.execSQL("DROP TABLE category;");
			onCreate(db);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);

			if (!db.isReadOnly()) {
				db.execSQL("PRAGMA foreign_keys = ON;");
			}
		}
	}
}
