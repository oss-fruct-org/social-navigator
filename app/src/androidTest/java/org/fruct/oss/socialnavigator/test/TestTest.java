package org.fruct.oss.socialnavigator.test;

import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class TestTest extends AndroidTestCase {
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
/*
	public void testOk() throws Exception {
		File cacheDirectory = new File(getContext().getCacheDir(), "test-cache");

		int cacheSize = 10 * 1024 * 1024; // 10 MiB
		Cache cache = new Cache(cacheDirectory, cacheSize);

		OkHttpClient client = new OkHttpClient();
		client.setCache(cache);

		Request request = new Request.Builder()
				.url("http://kappa.cs.petrsu.ru/~ivashov/qwe.txt")
				.build();

		Call call = client.newCall(request);
		Response response = call.execute();
		assertTrue(response.isSuccessful());
		System.err.println("code " + response.cacheResponse().code());
		assertEquals("qwee\n", response.body().string());
	}*/
}
