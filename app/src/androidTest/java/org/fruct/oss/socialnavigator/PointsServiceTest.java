package org.fruct.oss.socialnavigator;

import android.content.Intent;
import android.database.Cursor;
import android.test.MoreAsserts;
import android.test.RenamingDelegatingContext;
import android.test.ServiceTestCase;

import org.fruct.oss.socialnavigator.points.ArrayPointsProvider;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.Point;
import org.fruct.oss.socialnavigator.points.PointsService;

import java.util.List;

public class PointsServiceTest extends ServiceTestCase<PointsService> {
	private PointsService service;
	private Cursor cursor;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getContext().deleteDatabase("points-db");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		if (cursor != null) {
			assertTrue(cursor.isClosed());
		}

		service = null;
		cursor = null;
	}

	public PointsServiceTest() {
		super(PointsService.class);
	}

	private void initTestService() {
		Intent intent = new Intent(getContext(), PointsService.class);
		intent.putExtra("test", true);
		startService(intent);

		assertNotNull(getService());
		service = getService();
	}

	public void testStartService() {
		initTestService();
		assertNotNull(getService());
	}

	public void testAddCategories() {
		initTestService();
		ArrayPointsProvider provider = new ArrayPointsProvider(Point.TEST_PROVIDER);
		provider.setCategories("aaa", "bbb", "ccc");
		service.addPointsProvider(provider);
		service.refreshProviders();
		service.awaitBackgroundTasks();

		List<Category> categories = service.queryList(service.requestCategories());
		assertEquals("aaa", categories.get(0).getName());
		assertEquals("bbb", categories.get(1).getName());
		assertEquals("ccc", categories.get(2).getName());
	}

	public void testEmptyCategories() {
		initTestService();
		MoreAsserts.assertEmpty(service.queryList(service.requestCategories()));
	}

}
