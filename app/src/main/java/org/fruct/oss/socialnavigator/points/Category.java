package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Category implements Parcelable {
	private final String name;
	private final String description;
	private final String url;
	private final String iconUrl;
	private final int id;

	public Category(String name, String description, String url, String iconUrl, int id) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.id = id;
		this.iconUrl = iconUrl;
	}

	public Category(Cursor cursor, int offset) {
		this.id = cursor.getInt(offset);
		this.name = cursor.getString(offset + 1);
		this.description = cursor.getString(offset + 2);
		this.url = cursor.getString(offset + 3);
		this.iconUrl = cursor.getString(offset + 4);
	}

	public Category(Parcel source) {
		name = source.readString();
		description = source.readString();
		url = source.readString();
		iconUrl = source.readString();
		id = source.readInt();
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
	
	public String getIconUrl() {
		return iconUrl;
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Category category = (Category) o;

		if (id != category.id) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(description);
		dest.writeString(url);
		dest.writeString(iconUrl);
		dest.writeInt(id);
	}

	public static final Creator<Category> CREATOR = new Creator<Category>() {
		@Override
		public Category createFromParcel(Parcel source) {
			return new Category(source);
		}

		@Override
		public Category[] newArray(int size) {
			return new Category[size];
		}
	};
}
