package de.tuberlin.cit.livescale.job.task;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupVideoFrameChannelSelector;
import de.tuberlin.cit.livescale.job.util.decoder.VideoDecoder;
import eu.stratosphere.nephele.template.Collector;
import eu.stratosphere.nephele.template.IoCTask;
import eu.stratosphere.nephele.template.LastRecordReadFromWriteTo;
import eu.stratosphere.nephele.template.ReadFromWriteTo;

import java.util.HashMap;
import java.util.Map;

public final class DecoderTask extends IoCTask {

	private final GroupVideoFrameChannelSelector channelSelector = new GroupVideoFrameChannelSelector();
	private final Map<Long, VideoDecoder> streamId2Decoder = new HashMap<Long, VideoDecoder>();

	@Override
	public void setup() {
		initReader(0, Packet.class);
		initWriter(0, VideoFrame.class, channelSelector);
	}

	@ReadFromWriteTo(readerIndex = 0, writerIndex = 0)
	public void decode(Packet packet, Collector<VideoFrame> out) {
		try {
			VideoDecoder decoder = streamId2Decoder.get(packet.getStreamId());
			if (decoder == null) {
				decoder = new VideoDecoder(packet.getStreamId(),
						packet.getGroupId());
				streamId2Decoder.put(packet.getStreamId(), decoder);
			}

			VideoFrame frameToEmit;
			if (!packet.isEndOfStreamPacket()) {
				frameToEmit = decoder.decodePacket(packet);
			} else {
				frameToEmit = decoder.createEndOfStreamFrame();
				decoder.closeDecoder();
				streamId2Decoder.remove(packet.getStreamId());
			}

			if (frameToEmit != null) {
				out.emit(frameToEmit);
			}

			if (frameToEmit.isEndOfStreamFrame()) {
				out.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@LastRecordReadFromWriteTo(readerIndex = 0, writerIndex = 0)
	public void last(Collector<VideoFrame> out) {
		for (final VideoDecoder decoder : streamId2Decoder.values()) {
			decoder.closeDecoder();
		}
		streamId2Decoder.clear();
	}
}
