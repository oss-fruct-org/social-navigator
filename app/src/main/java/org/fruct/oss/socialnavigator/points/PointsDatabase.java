package org.fruct.oss.socialnavigator.points;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fruct.oss.socialnavigator.utils.Utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PointsDatabase implements Closeable {
	public static final int VERSION = 6;
	private final Context context;
	private final Helper helper;
	private final SQLiteDatabase db;

	private static final String[] COLUMNS_DISABILITY = { "_id", "name", "active" };
	private static final String[] COLUMNS_ID = { "_id" };
	private static final String[] COLUMNS_CATEGORY = { "_id", "name", "description", "url"};
	private static final String[] COLUMNS_POINT = { "_id", "name", "description", "url", "lat", "lon", "categoryId", "provider", "uuid", "difficulty" };

	public PointsDatabase(Context context) {
		this.context = context;
		this.helper = new Helper(context);
		this.db = helper.getWritableDatabase();

		/*File dbFile = new File(db.getPath());

		try {
			FileInputStream in = new FileInputStream(dbFile);
			FileOutputStream out = new FileOutputStream("/sdcard/debug/points.db");

			Utils.copyStream(in, out);

			in.close();
			out.close();
		} catch (Exception ex) {
		}*/
	}

	@Override
	public void close() {
		helper.close();
	}

	public void insertCategory(Category category) {
		if (category == null) {
			throw new IllegalArgumentException("Category can't be null");
		}

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
		if (point == null) {
			throw new IllegalArgumentException("Point can't be null");
		}

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

	public void setDisabilities(List<Disability> disabilities) {
		try {
			db.beginTransaction();

			// Load current disability state
			for (Disability disability : disabilities) {
				Cursor cursor = db.rawQuery("SELECT active FROM disability WHERE disability.name=?;",
						toArray(disability.getName()));
				if (cursor.moveToFirst()) {
					disability.setActive(cursor.getInt(0) != 0);
				}
				cursor.close();
			}

			db.execSQL("DELETE FROM disability_category;");
			db.execSQL("DELETE FROM disability;");

			for (Disability disability : disabilities) {
				insertDisability(disability);
			}

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	private void insertDisability(Disability disability) {
		ContentValues cv = new ContentValues(2);
		cv.put("name", disability.getName());
		cv.put("active", disability.isActive());
		long insertedId = db.insert("disability", null, cv);

		for (int categoryId : disability.getCategories()) {
			ContentValues catCv = new ContentValues(2);
			catCv.put("categoryId", categoryId);
			catCv.put("disabilityId", (int) insertedId);
			db.insert("disability_category", null, catCv);
		}
	}

	public Cursor loadCategories() {
		return db.query("category", COLUMNS_CATEGORY, null, null, null, null, "name");
	}

	public Cursor loadPoints() {
		return db.rawQuery("SELECT DISTINCT point._id, point.name, point.description, point.url, " +
				"point.lat, point.lon, point.categoryId, point.provider, point.uuid, point.difficulty " +
				"FROM point INNER JOIN category ON point.categoryId = category._id " +
				"INNER JOIN disability_category ON disability_category.categoryId = category._id " +
				"INNER JOIN disability ON disability_category.disabilityId = disability._id " +
				"WHERE disability.active = 1", null);

		//return db.query("point", COLUMNS_POINT, null, null, null, null, null);
	}

	public Cursor loadDisabilities() {
		return db.query("disability", COLUMNS_DISABILITY, null, null, null, null, null);
	}

	public void setDisabilityState(Disability disability, boolean isActive) {
		if (disability.getDbId() == -1) {
			throw new IllegalArgumentException("Trying change state of non stored disability");
		}

		ContentValues cv = new ContentValues(1);
		cv.put("active", isActive);
		db.update("disability", cv, "_id=?", toArray(disability.getDbId()));
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
					"(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"name TEXT, " +
					"description TEXT, " +
					"url TEXT, " +
					"lat INTEGER, " +
					"lon INTEGER, " +
					"categoryId INTEGER, " +
					"provider TEXT, " +
					"uuid TEXT, " +
					"difficulty INTEGER, " +
					"FOREIGN KEY(categoryId) REFERENCES category(_id));");

			db.execSQL("CREATE TABLE disability " +
					"(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
					"active INTEGER, " +
					"name TEXT);");

			db.execSQL("CREATE TABLE disability_category " +
					"(_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"categoryId INTEGER," +
					"disabilityId INTEGER, " +
					"FOREIGN KEY(disabilityId) REFERENCES disability(_id));");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion != newVersion) {
				db.execSQL("DROP TABLE point;");
				db.execSQL("DROP TABLE category;");
				db.execSQL("DROP TABLE disability;");
				db.execSQL("DROP TABLE disability_category;");
				onCreate(db);
			}
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
