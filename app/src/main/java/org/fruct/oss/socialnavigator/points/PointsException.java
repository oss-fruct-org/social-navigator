package org.fruct.oss.socialnavigator.points;

import java.io.IOException;

public class PointsException extends Exception {
	public PointsException(String s, Throwable ex) {
		super(s, ex);
	}

	public PointsException(String s) {
		super(s);
	}
}
