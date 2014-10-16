package org.fruct.oss.socialnavigator.test;

import android.content.Context;
import android.test.AndroidTestCase;

import org.fruct.oss.socialnavigator.content.NetworkContentItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ContentParserTest extends AndroidTestCase {
	private Context context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = getContext().createPackageContext("org.fruct.oss.socialnavigator.test", Context.CONTEXT_IGNORE_SECURITY);

	}

	public void testCorrectFile() throws IOException {
		InputStream input = context.getAssets().open("root-correct.xml");

		NetworkContentItem[] items = NetworkContentItem.parse(new InputStreamReader(input));
		input.close();

		assertEquals(2, items.length);

		NetworkContentItem item1 = items[0];
		assertEquals("mapsforge-map", item1.getType());
		assertEquals("adygeya.osm.pbf.map", item1.getName());
		assertEquals("Республика Адыгея", item1.getDescription());
		assertEquals(5553686, item1.getSize());
		assertEquals(4049899, item1.getDownloadSize());
		assertEquals("gzip", item1.getCompression());
		assertEquals("https://dl.dropbox.com/sh/fww6efgazlmyb7i/AADNLIQKJMe8bWWW9LWyJAKwa/adygeya.osm.pbf.map.gz", item1.getUrl());
		assertEquals("f04bd34ff1552b6db68d697e2d91b0bd6735ea15", item1.getHash());
		assertEquals("1beae076da5059af890fbfbf39fa8aacf00a65e6", item1.getRegionId());

		NetworkContentItem item2 = items[1];
		assertEquals(1537653, item2.getSize());
		assertEquals(1537653, item2.getDownloadSize());
		assertNull(item2.getCompression());
	}

	public void testErrorFile() throws Exception {
		InputStream input = context.getAssets().open("root-error.xml");

		boolean thrown = false;
		try {
			NetworkContentItem[] items = NetworkContentItem.parse(new InputStreamReader(input));
		} catch (Exception ex) {
			thrown = true;
		}

		assertTrue(thrown);

		input.close();
	}

	public void testEmptyFile() throws IOException {
		InputStream input = context.getAssets().open("root-empty.xml");

		NetworkContentItem[] items = NetworkContentItem.parse(new InputStreamReader(input));
		input.close();

		assertEquals(1, items.length);

		NetworkContentItem item1 = items[0];
		assertEquals("", item1.getType());
		assertEquals("", item1.getName());
		assertEquals("", item1.getDescription());
	}
}
