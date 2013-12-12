package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.event.StreamAnnounceEvent;
import de.tuberlin.cit.livescale.job.event.StreamAnnounceReplyEvent;
import de.tuberlin.cit.livescale.job.mapper.EncoderMapper;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupedPacketChannelSelector;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.event.task.EventListener;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public final class EncoderTask extends AbstractTask implements EventListener {

	private RecordReader<VideoFrame> reader;

	private RecordWriter<Packet> packetWriter;

	private GroupedPacketChannelSelector channelSelector = new GroupedPacketChannelSelector();

	private Mapper<VideoFrame, Packet> mapper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.reader = new RecordReader<VideoFrame>(this, VideoFrame.class);
		this.reader.subscribeToEvent(this, StreamAnnounceEvent.class);
		this.packetWriter = new RecordWriter<Packet>(this, Packet.class, channelSelector);
		this.packetWriter.subscribeToEvent(this, StreamAnnounceReplyEvent.class);
		this.mapper = new EncoderMapper(getTaskConfiguration().getString(
				VideoEncoder.ENCODER_OUTPUT_FORMAT, "flv"));

		getEnvironment().registerMapper(this.mapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke() throws Exception {

		final Queue<Packet> outputCollector = this.mapper.getOutputCollector();

		try {
			while (this.reader.hasNext()) {

				this.mapper.map(this.reader.next());

				boolean shouldFlush = false;
				while (!outputCollector.isEmpty()) {
					Packet packetToEmit = outputCollector.poll();
					this.packetWriter.emit(packetToEmit);
					shouldFlush = shouldFlush || packetToEmit.isEndOfStreamPacket();
				}

				if (shouldFlush) {
					this.packetWriter.flush();
				}
			}
		} finally {
			this.mapper.close();
		}

		while (!outputCollector.isEmpty()) {
			this.packetWriter.emit(outputCollector.poll());
		}
	}

	@Override
	public void eventOccurred(AbstractTaskEvent event) {
		try {
			if (event instanceof StreamAnnounceEvent) {
				packetWriter.publishEvent(event);
			} else if (event instanceof StreamAnnounceReplyEvent) {
				reader.publishEvent(event);
			}
		} catch (Exception e) {
		}
	}
}
