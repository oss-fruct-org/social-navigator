package org.fruct.oss.socialnavigator.utils;

public final class Timer {
	private long accumulated = 0;
	private long time = -1;

	public final Timer start() {
		time = System.nanoTime();
		return this;
	}

	public final Timer stop() {
		if (time < 0)
			throw new IllegalStateException("Timer haven't been started");
		accumulated += System.nanoTime() - time;
		time = -1;
		return this;
	}

	public double getSeconds() {
		return accumulated / 1e9;
	}

	public Timer reset() {
		accumulated = 0;
		time = -1;
		return this;
	}
}