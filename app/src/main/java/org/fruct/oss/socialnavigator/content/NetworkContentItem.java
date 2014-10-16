package org.fruct.oss.socialnavigator.content;

import android.util.Xml;

import static org.fruct.oss.socialnavigator.utils.Utils.readText;
import static org.fruct.oss.socialnavigator.utils.Utils.readNumber;
import static org.fruct.oss.socialnavigator.utils.Utils.skip;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class NetworkContentItem implements ContentItem {
	private String name;

	private String type;

	private int size;

	private Url url;

	private String hash;

	private String description;

	private String regionId;

	public int getDownloadSize() {
		return url.size == -1 ? size : url.size;
	}

	public String getUrl() {
		return url.url;
	}

	public String getHash() {
		return hash;
	}

	public String getDescription() {
		return description;
	}

	public String getCompression() {
		return url.compression;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	public int getSize() {
		return size;
	}

	@Override
	public String getRegionId() {
		return regionId;
	}

	@Override
	public boolean isDownloadable() {
		return true;
	}

	@Override
	public boolean isReadonly() {
		return true;
	}

	@Override
	public String getStorage() {
		return getClass().getName();
	}


	@Override
	public InputStream loadContentItem() throws IOException {
		return null;
	}

	public static NetworkContentItem[] parse(InputStreamReader reader) {
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

	private static NetworkContentItem[] readContent(XmlPullParser parser) throws IOException, XmlPullParserException {
		ArrayList<NetworkContentItem> items = new ArrayList<NetworkContentItem>();

		parser.require(XmlPullParser.START_TAG, null, "content");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String name = parser.getName();
			if (name.equals("file")) {
				items.add(readFile(parser));
			} else {
				skip(parser);
			}
		}
		parser.require(XmlPullParser.END_TAG, null, "content");

		return items.toArray(new NetworkContentItem[items.size()]);
	}

	private static NetworkContentItem readFile(XmlPullParser parser) throws IOException, XmlPullParserException {
		NetworkContentItem item = new NetworkContentItem();

		parser.require(XmlPullParser.START_TAG, null, "file");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tag = parser.getName();
			if (tag.equals("type")) {
				item.type = readText(parser);
			} else if (tag.equals("name")) {
				item.name = readText(parser);
			} else if (tag.equals("description")) {
				item.description = readText(parser);
			} else if (tag.equals("size")) {
				item.size = readNumber(parser);
			} else if (tag.equals("url")) {
				item.url = readUrl(parser);
			} else if (tag.equals("hash")) {
				item.hash = readText(parser);
			} else if (tag.equals("region-id")) {
				item.regionId = readText(parser);
			}
		}

		return item;
	}

	private static Url readUrl(XmlPullParser parser) throws IOException, XmlPullParserException {
		Url url = new Url();
		parser.require(XmlPullParser.START_TAG, null, "url");

		url.compression = parser.getAttributeValue(null, "compression");
		String sizeStr = parser.getAttributeValue(null, "size");

		if (sizeStr != null) {
			url.size = Integer.parseInt(sizeStr);
		}

		url.url = readText(parser).trim();

		return url;
	}

	private static class Url {
		String compression;
		int size = -1;
		String url;
	}
}
