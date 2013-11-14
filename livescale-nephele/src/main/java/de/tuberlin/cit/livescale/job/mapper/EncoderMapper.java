package de.tuberlin.cit.livescale.job.mapper;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import eu.stratosphere.nephele.execution.Mapper;

public final class EncoderMapper implements Mapper<VideoFrame, Packet> {

	private static final Log LOG = LogFactory.getLog(EncoderMapper.class);

	private final HashMap<Long, VideoEncoder> streamId2Encoder = new HashMap<Long, VideoEncoder>();

	private final Queue<Packet> outputCollector = new ArrayBlockingQueue<Packet>(8192);

	private final String encoderOutputFormat;

	public EncoderMapper(String encoderOutputFormat) {
		this.encoderOutputFormat = encoderOutputFormat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void map(final VideoFrame input) throws Exception {

		VideoEncoder encoder = this.streamId2Encoder.get(input.streamId);
		if (encoder == null) {
			encoder = new VideoEncoder(input.streamId, input.groupId);
			this.streamId2Encoder.put(input.streamId, encoder);
			final Packet headerPacket = encoder.init(encoderOutputFormat);
			if (headerPacket != null) {
				this.outputCollector.add(headerPacket);
			}
		}

		if (!input.isEndOfStreamFrame()) {
			final Packet packet = encoder.encodeFrame(input);
			if (packet != null) {
				this.outputCollector.add(packet);
			}
		} else {
			final Packet packet = encoder.closeVideoEncoder();
			if (packet != null) {
				this.outputCollector.add(packet);
			}
			final Packet eofPacket = createEndOfStreamPacket(input.streamId, input.groupId);
			this.outputCollector.add(eofPacket);
			this.streamId2Encoder.remove(input.streamId);
		}
	}

	private Packet createEndOfStreamPacket(final long streamId, final long groupId) {

		LOG.debug(String.format("Creating end of stream packet for stream %d", streamId));

		final Packet endOfStreamPacket = new Packet(streamId, groupId, 0, null);
		endOfStreamPacket.markAsEndOfStreamPacket();

		return endOfStreamPacket;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void close() {

		for (final Entry<Long, VideoEncoder> entry : streamId2Encoder.entrySet()) {
			try {
				final VideoEncoder encoder = entry.getValue();
				Packet packet = encoder.closeVideoEncoder();
				this.outputCollector.add(packet);

				Packet eofPacket = createEndOfStreamPacket(encoder.getStreamId(), encoder.getGroupId());
				this.outputCollector.add(eofPacket);
			} catch (Exception e) {
			}
		}

		this.streamId2Encoder.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Queue<Packet> getOutputCollector() {

		return this.outputCollector;
	}

}
