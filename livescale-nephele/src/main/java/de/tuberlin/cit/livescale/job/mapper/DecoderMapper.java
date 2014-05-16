package de.tuberlin.cit.livescale.job.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.decoder.VideoDecoder;
import eu.stratosphere.nephele.execution.Mapper;

public final class DecoderMapper implements Mapper<Packet, VideoFrame> {

	private final Map<Long, VideoDecoder> streamId2Decoder = new HashMap<Long, VideoDecoder>();

	private final Queue<VideoFrame> outputCollector = new ArrayBlockingQueue<VideoFrame>(8192);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void map(final Packet input) throws Exception {

		VideoDecoder decoder = this.streamId2Decoder.get(input.getStreamId());
		if (decoder == null) {
			decoder = new VideoDecoder(input.getStreamId(), input.getGroupId());
			this.streamId2Decoder.put(input.getStreamId(), decoder);
		}

		VideoFrame frameToEmit = null;
		if (!input.isEndOfStreamPacket()) {
			frameToEmit = decoder.decodePacket(input);
		} else {
			frameToEmit = decoder.createEndOfStreamFrame();
			decoder.closeDecoder();
			this.streamId2Decoder.remove(input.getStreamId());
		}

		if (frameToEmit != null) {
			this.outputCollector.add(frameToEmit);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {

		for (final VideoDecoder decoder : this.streamId2Decoder.values()) {
			decoder.closeDecoder();
		}
		this.streamId2Decoder.clear();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Queue<VideoFrame> getOutputCollector() {

		return this.outputCollector;
	}

}
