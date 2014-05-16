package de.tuberlin.cit.livescale.job.mapper;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.overlay.LogoOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TimeOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TwitterOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.VideoOverlay;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.execution.Mapper;

public final class OverlayMapper implements Mapper<VideoFrame, VideoFrame> {

	private OverlayProvider[] overlayProviders;

	private final Queue<VideoFrame> outputCollector = new ArrayBlockingQueue<VideoFrame>(
			8192);

	private final Configuration conf;

	public String OVERLAY_PROVIDER_SEQUENCE = "OVERLAY_PROVIDER_SEQUENCE";

	public String DEFAULT_OVERLAY_PROVIDER_SEQUENCE = "time";

	public OverlayMapper(Configuration conf) {
		this.conf = conf;
	}

	public void startOverlays() throws Exception {
		String[] overlayProviderSequence = this.conf.getString(
				this.OVERLAY_PROVIDER_SEQUENCE,
				this.DEFAULT_OVERLAY_PROVIDER_SEQUENCE).split("[,|]");

		this.overlayProviders = new OverlayProvider[overlayProviderSequence.length];

		for (int i = 0; i < overlayProviderSequence.length; i++) {
			String currProvider = overlayProviderSequence[i];

			if (currProvider.equals("time")) {
				this.overlayProviders[i] = new TimeOverlayProvider();
			} else if (currProvider.equals("logo")) {
				this.overlayProviders[i] = new LogoOverlayProvider(this.conf);
			} else if (currProvider.equals("twitter")) {
				this.overlayProviders[i] = new TwitterOverlayProvider();
			} else {
				throw new Exception(String.format(
						"Unknown overlay provider: %s", currProvider));
			}
		}

		// Start the overlay providers before consuming the stream
		for (final OverlayProvider overlayProvider : this.overlayProviders) {
			overlayProvider.start();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void map(final VideoFrame input) {

		if (!input.isEndOfStreamFrame()) {
			// Apply overlays to frame
			for (final OverlayProvider overlayProvider : this.overlayProviders) {
				final VideoOverlay videoOverlay = overlayProvider
						.getOverlayForStream(input.streamId);

				if (videoOverlay != null) {
					videoOverlay.draw(input);
				}
			}
		} else {
			for (final OverlayProvider overlayProvider : this.overlayProviders) {
				overlayProvider.dropOverlayForStream(input.streamId);
			}
		}

		this.outputCollector.add(input);
	}

	@Override
	public void close() {

		// Stop the overlay threads
		for (final OverlayProvider overlayProvider : this.overlayProviders) {
			overlayProvider.stop();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Queue<VideoFrame> getOutputCollector() {

		return this.outputCollector;
	}

}
