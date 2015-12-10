package org.fruct.oss.socialnavigator.points;

import android.database.Cursor;
import android.util.Xml;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

public class  Disability {
	private String name;
	private int[] categories;
	private int[] factors;
	private int dbId = -1;
	private boolean isActive;

	public Disability(String name, int[] categories, int[] factors) {
		this.name = name;
		this.categories = categories;
		 this.factors = factors;
	}

	public Disability(String name, int[] categories, int dbId) {
		this.name = name;
		this.categories = categories;
		this.dbId = dbId;
	}

	public Disability(Cursor cursor) {
		dbId = cursor.getInt(0);
		name = cursor.getString(1);
		isActive = cursor.getInt(2) != 0;
	}

	public String getName() {
		return name;
	}

	public int[] getCategories() {
		if (categories == null) {
			throw new IllegalStateException("Can't get categories of incomplete object");
		}
		return categories;
	}

	public int[] getFactors() {
		if (factors == null) {
			throw new IllegalStateException("Can't get factors of incomplete object");
		}
		return factors;
	}
	public int getFactor(int cat, int cost) {
		if(cost == 0)
		{
			return 0;
		}
		if (factors == null) {
			throw new IllegalStateException("Can't get factors of incomplete object");
		}
		int res = -1;

		for (int i = 0; i < categories.length;i++) {
			if(categories[i] == cat) {
				res = i*5 + cost-1;
				break;
			}
		}
		//System.out.print(res );
		//System.out.print(" ");
		//System.out.println( factors.length);
		if(res == -1 )
		{
			return 0;
		}
		return factors[res];
	}

	public int getDbId() {
		return dbId;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isActive() {
		return isActive;
	}

	public static List<Disability> parse(Reader reader) throws IOException, XmlPullParserException {
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(reader);
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

		parser.nextTag();
		return readDisabilities(parser);
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
				XmlUtil.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "disabilities");

		return disabilities;
	}

	private static Disability readDisability(XmlPullParser parser) throws IOException, XmlPullParserException {
		String name = null;
		TIntList categories = new TIntArrayList();
		TIntList factors    = new TIntArrayList();

		parser.require(XmlPullParser.START_TAG, null, "disability");

		while ((parser.next() != XmlPullParser.END_TAG)) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			String tag = parser.getName();
			if (tag.equals("name")) {
				name = XmlUtil.readText(parser);
			} else if (tag.equals("category")) {
				categories.add(XmlUtil.readNumber(parser));
				//factors.add(1);//test
				//factors.add(2);//test
				//factors.add(3);//test
				//factors.add(4);//test
				//factors.add(5);//test
			} else if (tag.equals("factor")){
				factors.add(XmlUtil.readNumber(parser));
			} else {
				XmlUtil.skip(parser);
			}
		}

		if (name == null || categories.isEmpty()) {
			throw new XmlPullParserException("Incomplete disability");
		}

		return new Disability(name, categories.toArray(), factors.toArray());
	}
}
