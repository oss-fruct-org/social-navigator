package org.fruct.oss.socialnavigator.gets;

import android.support.annotation.NonNull;

import org.fruct.oss.socialnavigator.points.Disability;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class DisabilitiesTask extends QueryTask<List<Disability>> {
	@Override
	protected String getPostQuery() {
		return null;
	}

	@NonNull
	@Override
	protected String getRequestUrl() {
		return DISABILITIES_LIST;
	}

	@Override
	protected List<Disability> parseContent(String response) throws ParseException {
		try {
			return Disability.parse(new StringReader(response));
		} catch (IOException e) {
			throw new ParseException("StringReader shouldn't cause exception", e);
		} catch (XmlPullParserException e) {
			throw new ParseException("Invalid disabilities XML", e);
		}
	}
}
