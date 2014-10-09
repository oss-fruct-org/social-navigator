package org.fruct.oss.socialnavigator.utils;

import android.os.Parcel;
import android.os.Parcelable;

import org.osmdroid.util.GeoPoint;

import java.io.Serializable;

public class Turn implements Serializable, Parcelable {
	private Space.Point point;
	private int turnSharpness;
	private int turnDirection;

	public Turn(Space.Point point, int turnSharpness, int turnDirection) {

		this.point = point;
		this.turnSharpness = turnSharpness;
		this.turnDirection = turnDirection;
	}

	public Turn(Parcel source) {
		point = source.readParcelable(GeoPoint.class.getClassLoader());
		turnSharpness = source.readInt();
		turnDirection = source.readInt();
	}

	public Space.Point getPoint() {
		return point;
	}

	public int getTurnSharpness() {
		return turnSharpness;
	}

	public int getTurnDirection() {
		return turnDirection;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Turn turn = (Turn) o;

		if (turnDirection != turn.turnDirection) return false;
		if (turnSharpness != turn.turnSharpness) return false;
		if (!point.equals(turn.point)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = point != null ? point.hashCode() : 0;
		result = 31 * result + turnSharpness;
		result = 31 * result + turnDirection;
		return result;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(point, 0);
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
