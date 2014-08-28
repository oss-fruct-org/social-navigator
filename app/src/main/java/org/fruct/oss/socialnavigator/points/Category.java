package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;

public class Category {
	private final String name;
	private final String description;
	private final String url;
	private final int id;

	public Category(String name, String description, String url, int id) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.id = id;
	}

	public Category(Cursor cursor) {
		this.id = cursor.getInt(0);
		this.name = cursor.getString(1);
		this.description = cursor.getString(2);
		this.url = cursor.getString(3);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getUrl() {
		return url;
	}

	public int getId() {
		return id;
	}


}
