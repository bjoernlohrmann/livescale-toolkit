package de.tuberlin.cit.livescale.job.task;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.overlay.LogoOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.OverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TimeOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.TwitterOverlayProvider;
import de.tuberlin.cit.livescale.job.util.overlay.VideoOverlay;
import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.template.Collector;
import eu.stratosphere.nephele.template.IoCTask;
import eu.stratosphere.nephele.template.LastRecordReadFromWriteTo;
import eu.stratosphere.nephele.template.ReadFromWriteTo;

import java.io.IOException;

public class OverlayTask extends IoCTask {
  // Overlay mapper members
  private OverlayProvider[] overlayProviders;
  private Configuration conf;
  public String OVERLAY_PROVIDER_SEQUENCE = "OVERLAY_PROVIDER_SEQUENCE";
  public String DEFAULT_OVERLAY_PROVIDER_SEQUENCE = "time";

  @Override
  protected void setup() {
    initReader(0, VideoFrame.class);
    initWriter(0, VideoFrame.class);

    conf = getTaskConfiguration();
    this.overlayProviders = new OverlayProvider[2];
    this.overlayProviders[0] = new TimeOverlayProvider();

    try {
      this.overlayProviders[1] = new LogoOverlayProvider(conf);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Start the overlay providers before consuming the stream
    for (final OverlayProvider overlayProvider : this.overlayProviders) {
      if (overlayProvider != null) {
        overlayProvider.start();
      }
    }

    try {
      startOverlays();
    } catch (Exception e) {
      e.printStackTrace();
    }

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

  @ReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void overlay(VideoFrame frame, Collector<VideoFrame> out) {

    if (frame.isDummyFrame()) {
      out.emit(new VideoFrame());
      out.flush();
      return;
    }

    if (!frame.isEndOfStreamFrame()) {
      // Apply overlays to frame
      for (final OverlayProvider overlayProvider : this.overlayProviders) {
        if (overlayProvider != null) {
          final VideoOverlay videoOverlay = overlayProvider
              .getOverlayForStream(frame.streamId);

          if (videoOverlay != null) {
            videoOverlay.draw(frame);
          }
        }
      }
    } else {
      for (final OverlayProvider overlayProvider : this.overlayProviders) {
        overlayProvider.dropOverlayForStream(frame.streamId);
      }
    }

    out.emit(frame);

    if (frame.isEndOfStreamFrame()) {
      out.flush();
    }
  }

  @LastRecordReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void last(Collector<VideoFrame> out) {
    for (final OverlayProvider overlayProvider : this.overlayProviders) {
      overlayProvider.stop();
    }
  }

}
