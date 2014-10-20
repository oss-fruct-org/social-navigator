package org.fruct.oss.socialnavigator.utils;

import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
	private final String prefix;
	private AtomicInteger idx = new AtomicInteger(0);

	public NamedThreadFactory(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Thread newThread(@NonNull Runnable r) {
		Thread thread = new Thread(r);
		if (thread.getPriority() != Thread.NORM_PRIORITY)
			thread.setPriority(Thread.NORM_PRIORITY);

		if (thread.isDaemon())
			thread.setDaemon(false);

		thread.setName(prefix + "-" + idx.getAndIncrement());
		return thread;
	}
}
