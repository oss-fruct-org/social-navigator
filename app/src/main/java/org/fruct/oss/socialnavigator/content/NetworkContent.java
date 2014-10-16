package org.fruct.oss.socialnavigator.content;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.fruct.oss.socialnavigator.utils.Utils.readText;
import static org.fruct.oss.socialnavigator.utils.Utils.skip;

public class NetworkContent {
	private String[] includes;
	private NetworkContentItem[] items;

	public NetworkContent(List<String> includeList, List<NetworkContentItem> networkContentItemList) {
		this.includes = includeList.toArray(new String[includeList.size()]);
		this.items = networkContentItemList.toArray(new NetworkContentItem[networkContentItemList.size()]);
	}

	public String[] getIncludes() {
		return includes;
	}

	public NetworkContentItem[] getItems() {
		return items;
	}

	public static NetworkContent parse(InputStreamReader reader) {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(reader);
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

			parser.nextTag();
			return readContent(parser);
		} catch (XmlPullParserException ex) {
			throw new RuntimeException("Can't parser content data", ex);
		} catch (IOException ex) {
			throw new RuntimeException("Can't parser content data", ex);
		}
	}

	private static NetworkContent readContent(XmlPullParser parser) throws IOException, XmlPullParserException {
		ArrayList<NetworkContentItem> items = new ArrayList<NetworkContentItem>();
		ArrayList<String> includes = new ArrayList<String>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String name = parser.getName();
			if (name.equals("file")) {
				items.add(NetworkContentItem.readFile(parser));
			} else if (name.equals("include")) {
				includes.add(readText(parser));
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, "content");

		return new NetworkContent(includes, items);
	}
}
