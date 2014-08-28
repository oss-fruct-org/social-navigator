package org.fruct.oss.socialnavigator.points;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.io.IOException;

public class PointsDatabase implements Closeable {
	public static final int VERSION = 1;
	private final Context context;
	private final Helper helper;
	private final SQLiteDatabase db;

	private static final String[] COLUMNS_CATEGORY = { "_id", "name", "description", "url"};
	private static final String[] COLUMNS_POINT = { "_id", "name", "description", "url", "lat", "lon", "categoryId"};

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
		cv.put("_id", category.getId());
		cv.put("name", category.getName());
		cv.put("description", category.getDescription());
		cv.put("url", category.getUrl());
		db.insert("category", null, cv);
	}

	public void insertPoint(Point point) {
		ContentValues cv = new ContentValues(7);
		cv.put("name", point.getName());
		cv.put("description", point.getDescription());
		cv.put("url", point.getUrl());
		cv.put("lat", point.getLatE6());
		cv.put("lon", point.getLonE6());
		cv.put("categoryId", point.getCategoryId());
		cv.put("provider", point.getProvider());
		db.insert("point", null, cv);
	}

	public Cursor loadCategories() {
		return db.query("category", COLUMNS_CATEGORY, null, null, null, null, "name");
	}

	public Cursor loadPoints(Category category) {
		return db.query("point", COLUMNS_POINT, "categoryId=?", toArray(category.getId()), null, null, null);
	}

	public Cursor loadPoints(int categoryId) {
		return db.query("point", COLUMNS_POINT, "categoryId=?", toArray(categoryId), null, null, null);
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
					"FOREIGN KEY(categoryId) REFERENCES category(id));");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE point;");
			db.execSQL("DROP TABLE category;");
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
