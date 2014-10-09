package org.fruct.oss.socialnavigator;

import android.test.AndroidTestCase;

import ikm.Qwerty;

public class TestTest extends AndroidTestCase {
	public void testQwertyModule() {
		Qwerty qwerty = new Qwerty();
		assertEquals(4, qwerty.random());
	}
}
