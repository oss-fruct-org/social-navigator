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
	private final boolean published;

	public Category(String name, String description, String url, String iconUrl, int id, boolean published) {
		this.name = name;
		this.description = description;
		this.url = url;
		this.id = id;
		this.iconUrl = iconUrl;
		this.published = published;
	}

	public Category(Cursor cursor, int offset) {
		this.id = cursor.getInt(offset);
		this.name = cursor.getString(offset + 1);
		this.description = cursor.getString(offset + 2);
		this.url = cursor.getString(offset + 3);
		this.iconUrl = cursor.getString(offset + 4);
		this.published = cursor.getInt(offset + 5) != 0;
	}

	public Category(Parcel source) {
		this.name = source.readString();
		this.description = source.readString();
		this.url = source.readString();
		this.iconUrl = source.readString();
		this.id = source.readInt();
		this.published = source.readInt() != 0;
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

	public boolean isPublished() {
		return published;
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
		dest.writeInt(published ? 1 : 0);
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
