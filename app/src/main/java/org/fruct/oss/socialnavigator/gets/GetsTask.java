package org.fruct.oss.socialnavigator.gets;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Xml;

import org.fruct.oss.mapcontent.content.utils.XmlUtil;
import org.fruct.oss.socialnavigator.parsers.IContent;
import org.fruct.oss.socialnavigator.utils.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringReader;

public abstract class GetsTask<T> extends QueryTask<T> {
	private int code;
	private String message;

	public int getCode() {
		return code;
	}
	public String getMessage() {
		return message;
	}

	@Override
	protected T parseContent(String response) throws ParseException {
		try {
			XmlPullParser parser = Xml.newPullParser();

			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(new StringReader(response));
			parser.nextTag();
			return readGetsResponse(parser);
		} catch (XmlPullParserException ex) {
			throw new ParseException("Invalid GeTS XML", ex);
		} catch (IOException ex) {
			throw new ParseException("StringReader shouldn't cause exception", ex);
		}
	}

	protected abstract T parseContent(XmlPullParser parser) throws IOException, XmlPullParserException;

	private T readGetsResponse(XmlPullParser parser) throws IOException, XmlPullParserException {
		T ret = null;

		parser.require(XmlPullParser.START_TAG, null, "response");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			switch (tagName) {
			case "status":
				readStatus(parser);
				break;

			case "content":
				ret = parseContent(parser);
				break;

			default:
				XmlUtil.skip(parser);
				break;
			}
		}

		return ret;
	}

	private void readStatus(XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, "status");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tagName = parser.getName();
			if (tagName.equals("code")) {
				parser.require(XmlPullParser.START_TAG, null, "code");
				this.code = Integer.parseInt(readText(parser));
				parser.require(XmlPullParser.END_TAG, null, "code");
			} else if (tagName.equals("message")) {
				parser.require(XmlPullParser.START_TAG, null, "message");
				this.message = readText(parser);
				parser.require(XmlPullParser.END_TAG, null, "message");
			}
		}
	}

	protected void createRequestTop(XmlSerializer xmlSerializer) throws IOException {
		xmlSerializer.startDocument("UTF-8", true);
		xmlSerializer.startTag(null, "request").startTag(null, "params");
	}

	protected void createRequestBottom(XmlSerializer xmlSerializer) throws IOException {
		xmlSerializer.endTag(null, "params").endTag(null, "request");
		xmlSerializer.endDocument();
	}


	protected String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
}
