package org.fruct.oss.socialnavigator.content;

import android.util.Xml;

import org.fruct.oss.socialnavigator.utils.Utils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class Metadata {
	private String regionId;

	private String name;

	private Map<String, String> description;

	public String getRegionId() {
		return regionId;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public static Metadata parse(Reader reader) {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(reader);
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

			parser.nextTag();
			return readMetadata(parser);
		} catch (XmlPullParserException ex) {
			throw new RuntimeException("Can't parse content data", ex);
		} catch (IOException ex) {
			throw new RuntimeException("Can't parse content data", ex);
		}

	}

	private static Metadata readMetadata(XmlPullParser parser) throws IOException, XmlPullParserException {
		Metadata metadata = new Metadata();
		metadata.description = new HashMap<String, String>();

		parser.require(XmlPullParser.START_TAG, null, "file");

		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG)
				continue;

			String tag = parser.getName();
			if (tag.equals("description")) {
				String lang = parser.getAttributeValue(null, "lang");
				if (lang == null) {
					lang = "en";
				}

				metadata.description.put(lang, Utils.readText(parser));
			} else if (tag.equals("name")) {
				metadata.name = Utils.readText(parser);
			} else 	if (tag.equals("region-id")) {
				metadata.regionId = Utils.readText(parser);
			} else {
				Utils.skip(parser);
			}
		}

		if (metadata.name == null || metadata.regionId == null) {
			throw new RuntimeException("Wrong xml file");
		}

		return metadata;
	}
}
