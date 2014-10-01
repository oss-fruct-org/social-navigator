package org.fruct.oss.socialnavigator.utils;

import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;

public class Turn implements Serializable, Parcelable {
	private GeoPoint geoPoint;
	private int turnSharpness;
	private int turnDirection;

	public Turn(GeoPoint geoPoint, int turnSharpness, int turnDirection) {

		this.geoPoint = geoPoint;
		this.turnSharpness = turnSharpness;
		this.turnDirection = turnDirection;
	}

	public Turn(Parcel source) {
		geoPoint = source.readParcelable(GeoPoint.class.getClassLoader());
		turnSharpness = source.readInt();
		turnDirection = source.readInt();
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public int getTurnSharpness() {
		return turnSharpness;
	}

	public int getTurnDirection() {
		return turnDirection;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(geoPoint, 0);
		dest.writeInt(turnSharpness);
		dest.writeInt(turnDirection);
	}

	public static final Creator<Turn> CREATOR = new Creator<Turn>() {
		@Override
		public Turn createFromParcel(Parcel source) {
			return new Turn(source);
		}

		@Override
		public Turn[] newArray(int size) {
			return new Turn[size];
		}
	};
}
