package de.tuberlin.cit.livescale.job.task;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupPacketChannelSelector;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import eu.stratosphere.nephele.template.Collector;
import eu.stratosphere.nephele.template.IoCTask;
import eu.stratosphere.nephele.template.LastRecordReadFromWriteTo;
import eu.stratosphere.nephele.template.ReadFromWriteTo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

public final class EncoderTask extends IoCTask {

  private GroupPacketChannelSelector channelSelector = new GroupPacketChannelSelector();
  private static final Log LOG = LogFactory.getLog(EncoderTask.class);
  private final HashMap<Long, VideoEncoder> streamId2Encoder = new HashMap<Long, VideoEncoder>();
  private String encoderOutputFormat;


  @Override
  protected void setup() {
    initReader(0, VideoFrame.class);
    initWriter(0, Packet.class, channelSelector);
    encoderOutputFormat = getTaskConfiguration().getString(VideoEncoder.ENCODER_OUTPUT_FORMAT, "flv");
  }

  @ReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void encode(VideoFrame frame, Collector<Packet> out) throws InterruptedException {

    if (frame.isDummyFrame()) {
      out.emit(new Packet());
      out.flush();
      return;
    }

    VideoEncoder encoder = streamId2Encoder.get(frame.streamId);
    if (encoder == null) {
      encoder = new VideoEncoder(frame.streamId, frame.groupId);
      streamId2Encoder.put(frame.streamId, encoder);
      final Packet headerPacket = encoder.init(encoderOutputFormat);
      if (headerPacket != null) {
        out.emit(headerPacket);
      }
    }

    if (!frame.isEndOfStreamFrame()) {
      final Packet packet = encoder.encodeFrame(frame);
      if (packet != null) {
        out.emit(packet);
      }
    } else {
      final Packet packet = encoder.closeVideoEncoder();
      if (packet != null) {
        out.emit(packet);
      }
      final Packet eofPacket = createEndOfStreamPacket(frame.streamId, frame.groupId);
      out.emit(eofPacket);
      out.flush();
      streamId2Encoder.remove(frame.streamId);
    }
  }

  private Packet createEndOfStreamPacket(final long streamId, final long groupId) {

    LOG.debug(String.format("Creating end of stream packet for stream %d", streamId));

    final Packet endOfStreamPacket = new Packet(streamId, groupId, 0, null);
    endOfStreamPacket.markAsEndOfStreamPacket();

    return endOfStreamPacket;
  }

  @LastRecordReadFromWriteTo(readerIndex = 0, writerIndex = 0)
  public void last(Collector<Packet> out) {
    for (final Map.Entry<Long, VideoEncoder> entry : streamId2Encoder.entrySet()) {
      try {
        final VideoEncoder encoder = entry.getValue();
        Packet packet = encoder.closeVideoEncoder();
        out.emit(packet);

        Packet eofPacket = createEndOfStreamPacket(encoder.getStreamId(), encoder.getGroupId());
        out.emit(eofPacket);
      } catch (Exception e) {
      }
    }

    streamId2Encoder.clear();
  }
}
