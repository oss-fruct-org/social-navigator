package org.fruct.oss.socialnavigator.parsers;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class AuthParameters implements IContent {
	private String clientId;
	private String scope;

	public String getClientId() {
		return clientId;
	}

	public String getScope() {
		return scope;
	}

	public static IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "content");
		AuthParameters content = new AuthParameters();

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();

			if (tagName.equals("client_id")) {
				content.clientId = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "client_id");
			} else if (tagName.equals("scope")) {
				content.scope = GetsResponse.readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "scope");
			} else {
				XmlUtil.skip(parser);
			}
		}

		return content;
	}
}
