package org.fruct.oss.socialnavigator.parsers;

import android.util.Xml;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class GetsResponse {
	private final static Logger log = LoggerFactory.getLogger(GetsResponse.class);
	private int code;
	private String message;

	private IContent content;

	public int getCode() {
		return code;
	}
	public String getMessage() {
		return message;
	}
	public IContent getContent() {
		return content;
	}


	private static interface ContentParser {
		IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException;
	}
	private static final Map<Class<? extends IContent>, ContentParser> contentParsers
			= new HashMap<Class<? extends IContent>, ContentParser>();
	static {
		contentParsers.put(Kml.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return Kml.parse(parser);
			}
		});

		contentParsers.put(AuthRedirectResponse.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return AuthRedirectResponse.parse(parser);
			}
		});

		contentParsers.put(TokenContent.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return TokenContent.parse(parser);
			}
		});
		contentParsers.put(CategoriesContent.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return CategoriesContent.parse(parser);
			}
		});

		contentParsers.put(AuthParameters.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return AuthParameters.parse(parser);
			}
		});

		contentParsers.put(UserInfo.class, new ContentParser() {
			@Override
			public IContent parse(XmlPullParser parser) throws IOException, XmlPullParserException {
				return UserInfo.parse(parser);
			}
		});

	}

	public static GetsResponse parse(String responseXml, Class<? extends IContent> contentClass) throws GetsException {
		try {
			//XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
			XmlPullParser parser = Xml.newPullParser();

			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new StringReader(responseXml));
			parser.nextTag();
			return readGetsResponse(parser, contentClass);
		} catch (XmlPullParserException | IOException e) {
			throw new GetsException(e);
		}
	}

	private static GetsResponse readGetsResponse(XmlPullParser parser, Class<? extends IContent> contentClass) throws IOException, XmlPullParserException {
		GetsResponse resp = new GetsResponse();
		parser.require(XmlPullParser.START_TAG, null, "response");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("status")) {
				readStatus(resp, parser);
			} else if (tagName.equals("content") && contentClass != null) {
				ContentParser contentParser = contentParsers.get(contentClass);
				if (contentParser == null) {
					XmlUtil.skip(parser);
				} else {
					resp.content = contentParser.parse(parser);
				}
			} else {
				XmlUtil.skip(parser);
			}
		}

		return resp;
	}

	private static void readStatus(GetsResponse out, XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "status");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("code")) {
				parser.require(XmlPullParser.START_TAG, null, "code");
				out.code = Integer.parseInt(readText(parser));
				parser.require(XmlPullParser.END_TAG, null, "code");
			} else if (tagName.equals("message")) {
				parser.require(XmlPullParser.START_TAG, null, "message");
				out.message = readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "message");
			}
		}
	}

	public static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
}
