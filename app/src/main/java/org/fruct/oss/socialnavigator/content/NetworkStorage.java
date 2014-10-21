package org.fruct.oss.socialnavigator.content;

import org.fruct.oss.socialnavigator.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkStorage implements ContentStorage {
	private static final Logger log = LoggerFactory.getLogger(NetworkStorage.class);

	private final String[] rootUrls;
	private List<ContentItem> items;

	public NetworkStorage(String[] rootUrls) {
		this.rootUrls = rootUrls;
	}

	@Override
	public void updateContentList() throws IOException {
		items = null;
		for (String contentUrl : rootUrls) {
			try {
				items = getContentList(new String[] {contentUrl}, new HashSet<String>());
				log.warn("Content root url {} successfully downloaded", contentUrl);

				break;
			} catch (IOException ex) {
				log.warn("Content root url {} unavailable", contentUrl);
			}
		}

		if (items == null) {
			throw new IOException("No one of remote content roots are available");
		}
	}

	private List<ContentItem> getContentList(String[] contentUrls, Set<String> visited) throws IOException {
		ArrayList<ContentItem> ret = new ArrayList<ContentItem>();

		int countSuccessful = 0;
		for (String url : contentUrls) {
			if (visited.contains(url)) {
				countSuccessful++;
				continue;
			}

			visited.add(url);
			InputStream conn = null;

			try {
				conn = loadContentItem(url);

				NetworkContent content = NetworkContent.parse(new InputStreamReader(conn));

				for (NetworkContentItem item : content.getItems()) {
					if (item.getType().equals(RemoteContentService.GRAPHHOPPER_MAP)) {
						item.setNetworkStorage(this);
						ret.add(item);
					}
				}

				countSuccessful++;

				ret.addAll(getContentList(content.getIncludes(), visited));
			} catch (IOException e) {
				log.warn("Content link " + url + " broken: ", e);
			} finally {
				if (conn != null)
					conn.close();
			}
		}

		if (countSuccessful == 0 && contentUrls.length > 0)
			throw new IOException("No one of remote content roots are available");

		return ret;
	}

	public InputStream loadContentItem(String urlStr) throws IOException {
		final HttpURLConnection conn = Utils.getConnection(urlStr);
		final InputStream input = conn.getInputStream();

		return new FilterInputStream(input) {
			@Override
			public void close() throws IOException {
				super.close();
				conn.disconnect();
			}
		};
	}

	@Override
	public List<ContentItem> getContentList() {
		return items;
	}
}
