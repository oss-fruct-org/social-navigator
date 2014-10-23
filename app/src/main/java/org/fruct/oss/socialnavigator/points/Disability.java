package org.fruct.oss.socialnavigator.points;

import android.util.Xml;

import org.fruct.oss.socialnavigator.utils.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

public class Disability {
	private String name;
	private int[] categories;

	public Disability(String name, int[] categories) {
		this.name = name;
		this.categories = categories;
	}

	public String getName() {
		return name;
	}

	public int[] getCategories() {
		return categories;
	}

	public static List<Disability> parse(Reader reader) {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(reader);
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

			parser.nextTag();
			return readDisabilities(parser);
		} catch (XmlPullParserException ex) {
			throw new RuntimeException("Can't parser content data", ex);
		} catch (IOException ex) {
			throw new RuntimeException("Can't parser content data", ex);
		}

	}

	private static List<Disability> readDisabilities(XmlPullParser parser) throws IOException, XmlPullParserException {
		List<Disability> disabilities = new ArrayList<Disability>();

		parser.require(XmlPullParser.START_TAG, null, "disabilities");

		while ((parser.next() != XmlPullParser.END_TAG)) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String tag = parser.getName();
			if (tag.equals("disability")) {
				disabilities.add(readDisability(parser));
			} else {
				Utils.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "disabilities");

		return disabilities;
	}

	private static Disability readDisability(XmlPullParser parser) throws IOException, XmlPullParserException {
		String name = null;
		TIntList categories = new TIntArrayList();

		parser.require(XmlPullParser.START_TAG, null, "disability");

		while ((parser.next() != XmlPullParser.END_TAG)) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String tag = parser.getName();
			if (tag.equals("name")) {
				name = Utils.readText(parser);
			} else if (tag.equals("category")) {
				categories.add(Utils.readNumber(parser));
			} else {
				Utils.skip(parser);
			}
		}

		if (name == null || categories.isEmpty()) {
			throw new XmlPullParserException("Incomplete disability");
		}

		return new Disability(name, categories.toArray());
	}
}
