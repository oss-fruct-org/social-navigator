package org.fruct.oss.socialnavigator.parsers;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.utils.GetsProperties;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoriesContent implements IContent {
	private List<Category> categories = new ArrayList<Category>();

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "categories");

		CategoriesContent content = new CategoriesContent();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("category")) {
				content.categories.add(parseCategory(parser));
			} else {
				XmlUtil.skip(parser);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "categories");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return content;
	}

	private static Category parseCategory(XmlPullParser parser) throws IOException, XmlPullParserException {
		long id = 0;
		String name = null;
		boolean published = false;

		String rawDescription = null;
		String rawUrl = null;

		parser.require(XmlPullParser.START_TAG, null, "category");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			switch (tagName) {
			case "id":
				id = Long.parseLong(GetsResponse.readText(parser));
				break;
			case "name":
				name = GetsResponse.readText(parser);
				break;
			case "description":
				rawDescription = GetsResponse.readText(parser);
				break;
			case "url":
				rawUrl = GetsResponse.readText(parser);
				break;
			case "published":
				published = Utils.isTrueString(GetsResponse.readText(parser));
				break;
			default:
				XmlUtil.skip(parser);
				break;
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
				(int) id, published);
	}

	public List<Category> getCategories() {
		return categories;
	}

	public static List<Category> filterByPrefix(List<Category> categories) {
		Pattern pattern = Pattern.compile("^(image|audio)\\..*");
		List<Category> matchedCategories = new ArrayList<Category>();

		for (Category category : categories) {
			if (category.getName() != null) {
				Matcher matcher = pattern.matcher(category.getName());
				if (matcher.matches()) {
					matchedCategories.add(category);
				}
			}
		}

		return matchedCategories;
	}
}
