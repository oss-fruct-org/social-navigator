package org.fruct.oss.socialnavigator.test;

import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import ikm.Qwerty;

public class TestTest extends AndroidTestCase {
	public void testQwertyModule() {
		Qwerty qwerty = new Qwerty();
		assertEquals(4, qwerty.random());
	}

	public void testExecutorTest() throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();

		for (int i = 0; i < 100; i++) {
			final AtomicInteger integer = new AtomicInteger(0);
			final CountDownLatch latch = new CountDownLatch(2);

			executor.submit(new Runnable() {
				@Override
				public void run() {
					integer.compareAndSet(0, 1);
					latch.countDown();
				}
			});

			executor.execute(new Runnable() {
				@Override
				public void run() {
					integer.compareAndSet(0, 2);
					latch.countDown();
				}
			});

			latch.await();
			assertEquals(1, integer.get());
		}
	}
}
