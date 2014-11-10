package org.fruct.oss.socialnavigator.test;

import android.content.Context;
import android.test.AndroidTestCase;

import org.fruct.oss.mapcontent.content.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MetadataParserTest extends AndroidTestCase {
	private Context context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = getContext().createPackageContext("org.fruct.oss.socialnavigator.test", Context.CONTEXT_IGNORE_SECURITY);

	}

	public void testCorrectFile() throws IOException {
		InputStream input = context.getAssets().open("metadata.xml");

		Metadata metadata = Metadata.parse(new InputStreamReader(input));
		input.close();

		assertEquals("70015d77052905b9c495229209117fb6d969a507", metadata.getRegionId());
		assertEquals("karel.osm.pbf.ghz", metadata.getName());
		assertEquals("Республика Карелия", metadata.getDescription().get("ru"));
		assertEquals("Karelia Republic", metadata.getDescription().get("en"));
		assertEquals(2, metadata.getDescription().size());
	}

}
