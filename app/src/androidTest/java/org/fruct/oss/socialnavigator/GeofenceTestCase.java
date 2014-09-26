package org.fruct.oss.socialnavigator;

import android.location.Location;
import android.os.Bundle;
import android.test.AndroidTestCase;
import static android.test.MoreAsserts.*;

import org.fruct.oss.socialnavigator.routing.GeofencesManager;
import org.fruct.oss.socialnavigator.routing.SimpleGeofencesManager;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTestCase extends AndroidTestCase implements GeofencesManager.GeofencesListener {
	private Bundle lastEnter;
	private Bundle lastExit;

	private int idx;
	private List<Event> enterList = new ArrayList<Event>();
	private List<Event> exitList = new ArrayList<Event>();

	// Created using http://www.gpsies.com/createTrack.do
	private static double[] TEST_TRACK_1 = {
			55.88323682,37.44332671,
			55.88338124,37.44312286,
			55.88353770,37.44297266,
			55.88364000,37.44276881,
			55.88372425,37.44269371,
			55.88377840,37.44264006,
			55.88386867,37.44257569,
			55.88393486,37.44243621,
			55.88399503,37.44237184,
			55.88408530,37.44226455,
			55.88416352,37.44218945,
			55.88422370,37.44211435,
			55.88428387,37.44201779,
			55.88439219,37.44192123,
			55.88446439,37.44179248,
			55.88453660,37.44169592,
			55.88460279,37.44156718,
			55.88470509,37.44148135,
			55.88478933,37.44139552,
			55.88486756,37.44120240,
			55.88496384,37.44109511,
			55.88504206,37.44099855,
			55.88518648,37.44081616,
			55.88529479,37.44071960,
			55.88540310,37.44063377,
			55.88546929,37.44059085,
			55.88550539,37.44039773,
			55.88559565,37.44025826,
			55.88566184,37.44022607,
			55.88570998,37.44010806,
			55.88584236,37.43995785,
			55.88590253,37.43982911,
			55.88602287,37.43975400,
			55.88608906,37.43968963,
			55.88618534,37.43957161,
			55.88626356,37.43947505,
			55.88636585,37.43931412,
			55.88644407,37.43936777,
			55.88654035,37.43928194,
			55.88662459,37.43919610,
			55.88669679,37.43904590,
			55.88680510,37.43886351,
			55.88687128,37.43870258,
			55.88697959,37.43853092,
			55.88706383,37.43845582,
			55.88711798,37.43838071,
			55.88722629,37.43826270,
			55.88731053,37.43815541,
			55.88735866,37.43808031,
			55.88743087,37.43799448,
			55.88751510,37.43790864,
			55.88759332,37.43776917,
			55.88767756,37.43765115,
			55.88774375,37.43754386,
			55.88780391,37.43745803,
			55.88788213,37.43740439,
			55.88795434,37.43728637,
			55.88803857,37.43721127,
			55.88810476,37.43714690,
			55.88818899,37.43703961,
			55.88830331,37.43694305,
			55.88838153,37.43686795,
			55.88844772,37.43680357,
			55.88856805,37.43676066,
			55.88865229,37.43677139,
			55.88873652,37.43677139,
			55.88882076,37.43677139,
			55.88889296,37.43674993,
			55.88897117,37.43672847,
	};

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		reset();
	}

	private Location createLocation(double lat, double lon) {
		Location loc = new Location("test");
		loc.setLatitude(lat);
		loc.setLongitude(lon);
		return loc;
	}

	private GeofencesManager createManager() {
		return new SimpleGeofencesManager();
	}

	private Bundle createBundle(int value) {
		Bundle bundle = new Bundle();
		bundle.putInt("value", value);
		return bundle;
	}

	public void testEmpty() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		gm.setLocation(createLocation(60, 40));

		assertNull(lastEnter);
		assertNull(lastExit);
	}

	public void testWrongToken() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		boolean thrown = false;
		try {
			gm.addGeofence(0, 60, 60, 20, null);
		} catch (IllegalArgumentException ex) {
			thrown = true;
		}

		assertTrue(thrown);
	}

	public void testSamePosition() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		int token = gm.createToken();
		gm.addGeofence(token, 60, 60, 30, new Bundle());
		gm.setLocation(createLocation(60, 60));

		assertNotNull(lastEnter);
		assertNull(lastExit);
		reset();

		gm.setLocation(createLocation(60, 50));
		assertNull(lastEnter);
		assertNotNull(lastExit);
	}

	public void testWay() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		int token = gm.createToken();
		gm.addGeofence(token, 55.883739, 37.442702, 30, createBundle(1));
		gm.addGeofence(token, 55.884420, 37.441800, 30, createBundle(2));
		gm.addGeofence(token, 55.884971, 37.441028, 40, createBundle(3));

		assertNull(lastExit);
		assertNull(lastEnter);

		for (int i = 0; i < TEST_TRACK_1.length; i += 2) {
			gm.setLocation(createLocation(TEST_TRACK_1[i], TEST_TRACK_1[i + 1]));
		}

		assertEquals(3, enterList.size());
		assertEquals(3, exitList.size());

		assertEquals(1, enterList.get(0).point.getInt("value"));
		assertEquals(2, enterList.get(1).point.getInt("value"));
		assertEquals(3, enterList.get(2).point.getInt("value"));

		assertEquals(1, exitList.get(0).point.getInt("value"));
		assertEquals(2, exitList.get(1).point.getInt("value"));
		assertEquals(3, exitList.get(2).point.getInt("value"));
	}

	public void testExitEnterOrder() {
		GeofencesManager gm = createManager();

		gm.addListener(this);

		int token = gm.createToken();
		gm.addGeofence(token, 10, 10, 10, createBundle(1));
		gm.addGeofence(token, 20, 20, 10, createBundle(2));

		gm.setLocation(createLocation(0, 0));
		assertEmpty(enterList);
		assertEmpty(exitList);

		gm.setLocation(createLocation(10, 10));
		assertNotNull(lastEnter);
		assertNull(lastExit);
		assertEquals(1, enterList.get(0).point.getInt("value"));
		reset();

		gm.setLocation(createLocation(20, 20));
		assertTrue(enterList.get(0).idx > exitList.get(0).idx);
		assertEquals(2, enterList.get(0).point.getInt("value"));
		assertEquals(1, exitList.get(0).point.getInt("value"));
	}

	public void testDistance() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		int token = gm.createToken();
		gm.addGeofence(token, 61.787380, 34.354333, 100, new Bundle());

		gm.setLocation(createLocation(61.788254, 34.351661));
		gm.setLocation(createLocation(61.788571, 34.352673));
		gm.setLocation(createLocation(61.789112, 34.354368));
		assertNull(lastExit);
		assertNull(lastEnter);

		gm.setLocation(createLocation(61.787419, 34.351647));
		gm.setLocation(createLocation(61.787909, 34.353655));
		assertNotNull(lastEnter);
		assertNull(lastExit);
		reset();

		gm.setLocation(createLocation(61.788628, 34.355876));
		assertNotNull(lastExit);
		assertNull(lastEnter);
	}

	public void testTokens() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		int t1 = gm.createToken();
		int t2 = gm.createToken();
		gm.addGeofence(t1, 10, 10, 100, new Bundle());
		gm.addGeofence(t2, 20, 20, 100, new Bundle());

		gm.setLocation(createLocation(10, 10));
		assertNotNull(lastEnter);
		gm.removeGeofences(t1);
		reset();

		gm.setLocation(createLocation(10, 10));
		assertNull(lastEnter);
		assertNull(lastExit);

		gm.setLocation(createLocation(20, 20));
		assertNull(lastExit);
		assertNotNull(lastEnter);
	}

	public void testRemove() {
		GeofencesManager gm = createManager();
		gm.addListener(this);

		int t1 = gm.createToken();
		int t2 = gm.createToken();
		gm.addGeofence(t1, 10, 10, 100, new Bundle());
		gm.addGeofence(t2, 20, 20, 100, new Bundle());

		gm.setLocation(createLocation(10, 10));
		gm.removeGeofences(t1);

		gm.addGeofence(t1, 10, 10, 100, new Bundle());
	}

	@Override
	public void geofenceEntered(Bundle data) {
		lastEnter = data;
		enterList.add(new Event(true, data, idx++));
	}

	@Override
	public void geofenceExited(Bundle data) {
		lastExit = data;
		exitList.add(new Event(false, data, idx++));
	}

	private void reset() {
		lastEnter = null;
		lastExit = null;
		enterList.clear();
		exitList.clear();
		idx = 0;
	}

	private static class Event {
		private Event(boolean isEnter, Bundle point, int idx) {
			this.isEnter = isEnter;
			this.point = point;
			this.idx = idx;
		}

		boolean isEnter;
		Bundle point;
		int idx;
	}
}
