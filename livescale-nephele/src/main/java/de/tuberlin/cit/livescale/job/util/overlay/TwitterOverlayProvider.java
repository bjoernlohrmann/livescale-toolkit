package de.tuberlin.cit.livescale.job.util.overlay;

import twitter4j.FilterQuery;
import twitter4j.Status;
import twitter4j.StatusAdapter;
import twitter4j.StatusStream;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.Authorization;
import twitter4j.auth.BasicAuthorization;
import eu.stratosphere.nephele.configuration.Configuration;

public final class TwitterOverlayProvider extends StatusAdapter implements OverlayProvider, Runnable {

	public static final String TWITTER_USERNAME_KEY = "overlay.twitter.username";

	public static final String TWITTER_PASSWORD_KEY = "overlay.twitter.password";

	public static final String TWITTER_KEYWORD_KEY = "overlay.twitter.keyword";

	private final StatusStream statusStream;

	private final Thread streamThread;

	private volatile boolean interrupted = false;

	private volatile TwitterVideoOverlay videoOverlay = null;
	
	public TwitterOverlayProvider(final Configuration conf) throws TwitterException {

		// First check the configuration
		final String username = conf.getString(TWITTER_USERNAME_KEY, null);
		if (username == null) {
			throw new IllegalArgumentException(
				"Task TwitterStreamSource must provide a username configuration entry");
		}

		final String password = conf.getString(TWITTER_PASSWORD_KEY, null);
		if (password == null) {
			throw new IllegalArgumentException(
				"Task TwitterStreamSource must provide a password configuration entry");
		}

		final String keyword = conf.getString(TWITTER_KEYWORD_KEY, null);
		if (keyword == null) {
			throw new IllegalArgumentException(
				"Task TwitterStreamSource must provide a keyword configuration entry");
		}

		// Initialize the twitter client
		final Authorization auth = new BasicAuthorization(username, password);
		final TwitterStream twitterStream = new TwitterStreamFactory().getInstance(auth);

		// Construct the query object
		final FilterQuery query = new FilterQuery();
		query.track(new String[] { keyword });

		// Construct the status stream
		this.statusStream = twitterStream.getFilterStream(query);

		// Construct a thread to receive stream events
		this.streamThread = new Thread(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStatus(final Status status) {
		
		try {
			this.videoOverlay = new TwitterVideoOverlay(status);
		} catch(Exception e) {
			//e.printStackTrace();
		}
	}

	@Override
	public VideoOverlay getOverlay() {
		
		return this.videoOverlay;
	}

	@Override
	public void stop() {

		this.interrupted = true;
		this.streamThread.interrupt();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {

		try {

			while (!this.interrupted) {

				this.statusStream.next(this);
			}
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		if (!this.streamThread.isAlive()) {
			this.streamThread.start();
		}
	}
}