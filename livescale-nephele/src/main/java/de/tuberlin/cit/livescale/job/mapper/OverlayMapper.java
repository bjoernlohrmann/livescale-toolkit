package de.tuberlin.cit.livescale.job.mapper;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.overlay.LogoOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TimeOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.VideoOverlay;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.execution.Mapper;

public final class OverlayMapper implements Mapper<VideoFrame, VideoFrame> {

	private final OverlayProvider[] overlayProviders;

	private final Queue<VideoFrame> outputCollector = new ArrayBlockingQueue<VideoFrame>(
			8192);

	public OverlayMapper(Configuration conf) {

		this.overlayProviders = new OverlayProvider[2];
		this.overlayProviders[0] = new TimeOverlayProvider();

		try {
			this.overlayProviders[1] = new LogoOverlayProvider(conf);
		} catch (IOException e) {
			e.printStackTrace();
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
				final VideoOverlay videoOverlay = overlayProvider.getOverlay();
				if (videoOverlay != null) {
					videoOverlay.draw(input);
				}
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
