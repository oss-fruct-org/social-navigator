package org.fruct.oss.socialnavigator.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Xml;

import org.fruct.oss.socialnavigator.parsers.GetsException;
import org.fruct.oss.socialnavigator.parsers.GetsResponse;
import org.fruct.oss.socialnavigator.points.Category;
import org.fruct.oss.socialnavigator.points.GetsProvider;
import org.fruct.oss.socialnavigator.settings.Preferences;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class PublishingTask extends AsyncTask<Void, Void, PublishingTask.Result> {
	private static final Logger log = LoggerFactory.getLogger(PublishingTask.class);

	@Nullable
	private final String authToken;
	private final Mode mode;
	private final Category category;

	protected Throwable throwable;

	public PublishingTask(Context context, Mode mode, Category category) {
		this.mode = mode;
		this.category = category;
		Preferences appPref = new Preferences(context);
		authToken = appPref.getGetsToken();
	}

	@Override
	protected Result doInBackground(Void... voids) {
		if (authToken == null) {
			return Result.ERROR;
		}

		String request = createPublishRequest(authToken, category.getId());
		try {
			String ret = Utils.downloadUrl(GetsProvider.GETS_SERVER + mode.mode, request);
			GetsResponse response = GetsResponse.parse(ret, null);

			if (response.getCode() == 0)
				return Result.SUCCESS;
			else if (response.getCode() == 2)
				return Result.ALREADY;
			else
				return Result.ERROR;
		} catch (IOException e) {
			log.error("Publishing error", e);
			throwable = e;
			return Result.ERROR;
		} catch (GetsException e) {
			log.error("Publishing error", e);
			throwable = e;
			return Result.ERROR;
		}
	}

	private String createPublishRequest(@NonNull String authToken, int categoryId) {
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();

		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag(null, "request").startTag(null, "params");
			serializer.startTag(null, "auth_token").text(authToken).endTag(null, "auth_token");
			serializer.startTag(null, "category_id").text(String.valueOf(categoryId)).endTag(null, "category_id");
			serializer.endTag(null, "params").endTag(null, "request");
			serializer.endDocument();

			return writer.toString();
		} catch (IOException ignore) {
			throw new RuntimeException("StringWriter throw IOException");
		}
	}

	public static enum Mode {
		PUBLISH("/publish.php"), UNPUBLISH("/unpublish.php");

		private final String mode;

		Mode(String mode) {
			this.mode = mode;
		}
	}

	public static enum Result {
		SUCCESS, ALREADY, ERROR
	}
}
