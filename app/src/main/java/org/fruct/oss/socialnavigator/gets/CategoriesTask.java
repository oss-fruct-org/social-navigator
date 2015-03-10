package org.fruct.oss.socialnavigator.gets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.parsers.*;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.utils.GetsProperties;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CategoriesTask extends GetsTask<List<Category>> {
	@Override
	protected List<Category> parseContent(XmlPullParser parser) throws IOException, XmlPullParserException {
		List<Category> ret = new ArrayList<>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "categories");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("category")) {
				ret.add(parseCategory(parser));
			} else {
				XmlUtil.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "categories");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return ret;
	}

	@Nullable
	@Override
	protected String getPostQuery() {
		return "<request><params/></request>";
	}

	@NonNull
	@Override
	protected String getRequestUrl() {
		return GETS_SERVER + "/getCategories.php";
	}

	private Category parseCategory(XmlPullParser parser) throws IOException, XmlPullParserException {
		long id = 0;
		String name = null;

		String rawDescription = null;
		String rawUrl = null;

		parser.require(XmlPullParser.START_TAG, null, "category");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("id")) {
				id = Long.parseLong(org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser));
			} else if (tagName.equals("name")) {
				name = org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser);
			} else if (tagName.equals("description")) {
				rawDescription = org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser);
			} else if (tagName.equals("url")) {
				rawUrl = org.fruct.oss.socialnavigator.parsers.GetsResponse.readText(parser);
			} else {
				XmlUtil.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "category");

		GetsProperties getsProperties = new GetsProperties();
		getsProperties.addJson("description", rawDescription);
		getsProperties.addJson("url", rawUrl);

		return new Category(name,
				getsProperties.getProperty("description", ""),
				getsProperties.getProperty("url", ""),
				getsProperties.getProperty("icon", ""),
				(int) id);
	}

}
