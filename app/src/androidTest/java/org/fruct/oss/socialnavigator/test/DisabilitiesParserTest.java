package org.fruct.oss.socialnavigator.test;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;

import org.fruct.oss.socialnavigator.points.Disability;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static android.test.MoreAsserts.*;

public class DisabilitiesParserTest extends AndroidTestCase {
	private Context context;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = getContext().createPackageContext("org.fruct.oss.socialnavigator.test", Context.CONTEXT_IGNORE_SECURITY);
	}

	public void testDisabilitiesParser() throws Exception {
		InputStream input = context.getAssets().open("disabilities.xml");
		List<Disability> disabilities = Disability.parse(new InputStreamReader(input));

		assertEquals(5, disabilities.size());

		Disability d0 = disabilities.get(0);
		assertEquals("aaa", d0.getName());
		MoreAsserts.assertEquals(new int[] {23, 24, 27, 28, 29}, d0.getCategories());

		Disability d1 = disabilities.get(1);
		assertEquals("bbb", d1.getName());
		MoreAsserts.assertEquals(new int[] {24, 25, 26, 29}, d1.getCategories());

		Disability d2 = disabilities.get(2);
		assertEquals("ccc", d2.getName());
		MoreAsserts.assertEquals(new int[] {23, 24, 27, 28, 29}, d2.getCategories());

		Disability d3 = disabilities.get(3);
		assertEquals("ddd", d3.getName());
		MoreAsserts.assertEquals(new int[] {25, 26}, d3.getCategories());

		Disability d4 = disabilities.get(4);
		assertEquals("eee", d4.getName());
		MoreAsserts.assertEquals(new int[] {25, 26}, d4.getCategories());
	}
}
