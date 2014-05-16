package de.tuberlin.cit.livescale.job.util.overlay;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.MessageListener;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverEmbedTweets;

public final class TwitterOverlayProvider extends StatusAdapter implements
		OverlayProvider {

	private static final Logger LOG = Logger
			.getLogger(TwitterOverlayProvider.class);

	private static final String CONFIG_FILE_NAME = "twitteroverlay-messaging.properties";

	private final TwitterStream twitterStream;

	private MessageCenter messageCenter;

	private FilterQuery query;

	private HashSet<String> trackedKeywords = new HashSet<String>();

	private HashMap<Long, String[]> keywordsByStream = new HashMap<Long, String[]>();

	private ConcurrentHashMap<Long, TwitterVideoOverlay> overlays = new ConcurrentHashMap<Long, TwitterVideoOverlay>();

	public TwitterOverlayProvider() {
		this.twitterStream = new TwitterStreamFactory().getInstance();
		this.twitterStream.addListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatus(final Status status) {
		try {
			BufferedImage userImage = fetchUserImage(status);

			String statusText = status.getText();
			for (Entry<Long, String[]> keywords : this.keywordsByStream
					.entrySet()) {
				for (String keyword : keywords.getValue()) {
					if (statusText.contains(keyword)) {
						this.overlays.put(keywords.getKey(),
								new TwitterVideoOverlay(status, userImage));
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	private BufferedImage fetchUserImage(Status status) {
		BufferedImage ret = null;

		try {
			String profileImageURL = status.getUser().getProfileImageURL();
			if (profileImageURL != null) {
				URL imageUrl = new URL(profileImageURL);
				final HttpURLConnection connection = (HttpURLConnection) imageUrl
						.openConnection();
				connection.setRequestMethod("GET");
				connection.addRequestProperty("Accept",
						"image/gif, image/x-xbitmap, image/jpeg, image/pjpeg");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(true);

				connection.connect();
				final InputStream is = connection.getInputStream();
				BufferedImage bi = ImageIO.read(is);
				if (bi != null) {
					if (bi.getHeight() >= TwitterVideoOverlay.BOX_HEIGHT) {
						double scaleFactor = TwitterVideoOverlay.BOX_HEIGHT
								/ ((double) bi.getHeight());
						bi = createResizedCopy(bi,
								(int) (bi.getWidth() * scaleFactor),
								(int) (bi.getHeight() * scaleFactor));
					}
				}
				ret = bi;
			}
		} catch (IOException e) {
			LOG.error("Failed to fetch user image of user "
					+ status.getUser().getName());
		}

		return ret;
	}

	private BufferedImage createResizedCopy(BufferedImage originalImage,
			int scaledWidth, int scaledHeight) {

		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaledBI.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();
		return scaledBI;
	}

	@Override
	public void stop() {
		this.twitterStream.cleanUp();
	}

	private void modifyFilterQuery(StreamserverEmbedTweets message) {
		LOG.info(String.format("Tracking keywords %s for sender %s",
				message.getTwitterFilterKeyword(),
				message.getSendEndpointToken()));

		long streamId = message.getSendEndpointToken().hashCode();
		String[] keywords = message.getTwitterFilterKeyword().split(",");
		this.keywordsByStream.put(streamId, keywords);

		int oldSize = this.trackedKeywords.size();
		for (String keyword : keywords) {
			this.trackedKeywords.add(keyword);
		}
		if (oldSize != this.trackedKeywords.size()) {
			// Construct the query object
			this.query.track(this.trackedKeywords.toArray(new String[0]));
			this.twitterStream.filter(this.query);
		}
	}

	private void setupMessaging() throws IOException {
		try {
			URL url = this.getClass().getClassLoader()
					.getResource(CONFIG_FILE_NAME);
			LOG.info(String.format("Loading messaging config from %s", url));
			InputStream is = this.getClass().getClassLoader()
					.getResourceAsStream(CONFIG_FILE_NAME);

			Properties properties = new Properties();
			properties.load(is);

			this.messageCenter = new MessageCenter(false, properties);

			synchronized (this.messageCenter) {
				this.messageCenter.addMessageListener(
						StreamserverEmbedTweets.class,
						new MessageListener<StreamserverEmbedTweets>() {
							@Override
							public void handleMessageReceived(
									StreamserverEmbedTweets message) {
								modifyFilterQuery(message);
							}
						});

			}
			this.messageCenter.startAllEndpoints();
		} catch (IOException e) {
			LOG.error("Could not start Message center", e);
			throw e;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#start()
	 */
	@Override
	public void start() {
		try {
			setupMessaging();
			this.query = new FilterQuery();
			this.query.language(new String[] { "en" });
		} catch (IOException e) {
			LOG.error(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#
	 * dropOverlayForStream(long)
	 */
	@Override
	public void dropOverlayForStream(long streamId) {
		this.overlays.remove(streamId);
		this.keywordsByStream.remove(streamId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider#
	 * getOverlayForStream(long)
	 */
	@Override
	public VideoOverlay getOverlayForStream(long streamId) {
		if (this.overlays.get(streamId) == null) {
			String[] keywords = { "obama" };
			this.keywordsByStream.put(streamId, keywords);

			int oldSize = this.trackedKeywords.size();
			for (String keyword : keywords) {
				this.trackedKeywords.add(keyword);
			}
			if (oldSize != this.trackedKeywords.size()) {
				// Construct the query object
				this.query.track(this.trackedKeywords.toArray(new String[0]));
				this.twitterStream.filter(this.query);
			}

		}
		return this.overlays.get(streamId);
	}
}