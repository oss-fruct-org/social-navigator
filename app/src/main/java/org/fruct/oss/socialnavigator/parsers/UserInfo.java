package org.fruct.oss.socialnavigator.parsers;

import android.content.SharedPreferences;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class UserInfo implements IContent {
	private String imageUrl;
	private String name;
	private String email;

	private boolean isTrustedUser;

	private UserInfo() {
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public boolean isTrustedUser() {
		return isTrustedUser;
	}

	public void save(SharedPreferences pref, String prefix) {
		pref.edit()
				.putString(prefix + "ui-imageUrl", imageUrl)
				.putString(prefix + "ui-name", name)
				.putString(prefix + "ui-email", email)
				.putBoolean(prefix + "ui-isTrustedUser", isTrustedUser)
				.putBoolean(prefix + "ui", true)
				.apply();
	}

	public static UserInfo load(SharedPreferences pref, String prefix) {
		if (!pref.contains(prefix + "ui")) {
			return null;
		}

		UserInfo userInfo = new UserInfo();
		userInfo.imageUrl = pref.getString(prefix + "ui-imageUrl", null);
		userInfo.name = pref.getString(prefix + "ui-name", null);
		userInfo.email = pref.getString(prefix + "ui-email", null);
		userInfo.isTrustedUser = pref.getBoolean(prefix + "ui-isTrustedUser", false);
		return userInfo;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, "userInfo");

		UserInfo userInfo = new UserInfo();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			switch (tagName) {
			case "isTrustedUser":
				userInfo.isTrustedUser = "true".equals(GetsResponse.readText(parser));
				break;

			case "image":
				userInfo.imageUrl = GetsResponse.readText(parser);
				break;

			case "name":
				userInfo.name = GetsResponse.readText(parser);
				break;

			case "email":
				userInfo.email = GetsResponse.readText(parser);
				break;

			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		parser.require(XmlPullParser.END_TAG, null, "userInfo");
		parser.nextTag();
		parser.require(XmlPullParser.END_TAG, null, "content");

		return userInfo;
	}
}
