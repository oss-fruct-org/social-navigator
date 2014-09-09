package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;

public class Point {
	public static final String LOCAL_PROVIDER = "local_provider";
	public static final String TEST_PROVIDER = "test_provider";
	public static final String GETS_PROVIDER = "gets_provider";

	private String name;
	private String description;
	private String url;
	private int latE6;
	private int lonE6;
	private int categoryId;
	private String provider;
	private String uuid;

	public Point(String name, String description, String url, double lat, double lon, int categoryId, String provider, String uuid) {
		this(name, description, url, (int) (lat * 1e6), (int) (lon * 1e6), categoryId, provider, uuid);
	}

	public Point(String name, String description, String url, int latE6, int lonE6, int categoryId, String provider, String uuid) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.latE6 = latE6;
		this.lonE6 = lonE6;
		this.categoryId = categoryId;
		this.provider = provider;
		this.uuid = uuid;
	}

	public Point(Cursor cursor) {
		this.name = cursor.getString(1);
		this.description = cursor.getString(2);
		this.url = cursor.getString(3);
		this.latE6 = cursor.getInt(4);
		this.lonE6 = cursor.getInt(5);
		this.categoryId = cursor.getInt(6);
		this.provider = cursor.getString(7);
		this.uuid = cursor.getString(8);
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

	public int getLatE6() {
		return latE6;
	}

	public int getLonE6() {
		return lonE6;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public String getProvider() {
		return provider;
	}

	public String getUuid() {
		return uuid;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Point point = (Point) o;

		if (!uuid.equals(point.uuid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
