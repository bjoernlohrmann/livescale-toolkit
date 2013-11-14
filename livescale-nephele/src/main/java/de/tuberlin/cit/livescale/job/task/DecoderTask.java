package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.event.StreamAnnounceEvent;
import de.tuberlin.cit.livescale.job.event.StreamAnnounceReplyEvent;
import de.tuberlin.cit.livescale.job.mapper.DecoderMapper;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.GroupedVideoFrameChannelSelector;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.event.task.EventListener;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public final class DecoderTask extends AbstractTask implements EventListener {

	private RecordWriter<VideoFrame> writer;

	private RecordReader<Packet> packetReader;

	private final GroupedVideoFrameChannelSelector channelSelector = new GroupedVideoFrameChannelSelector();

	private final Mapper<Packet, VideoFrame> mapper = new DecoderMapper();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.packetReader = new RecordReader<Packet>(this, Packet.class);
		this.packetReader.subscribeToEvent(this, StreamAnnounceEvent.class);
		this.writer = new RecordWriter<VideoFrame>(this, VideoFrame.class, channelSelector);
		this.writer.subscribeToEvent(this, StreamAnnounceReplyEvent.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke() throws Exception {

		getEnvironment().registerMapper(this.mapper);

		final Queue<VideoFrame> outputCollector = this.mapper.getOutputCollector();

		try {
			while (packetReader.hasNext()) {
				this.mapper.map(packetReader.next());

				boolean shouldFlush = false;
				while (!outputCollector.isEmpty()) {
					VideoFrame frameToEmit = outputCollector.poll();
					this.writer.emit(frameToEmit);
					shouldFlush = shouldFlush || frameToEmit.isEndOfStreamFrame();
				}

				if (shouldFlush) {
					this.writer.flush();
				}
			}
		} finally {
			this.mapper.close();
		}

		while (!outputCollector.isEmpty()) {
			this.writer.emit(outputCollector.poll());
		}
	}

	@Override
	public void eventOccurred(AbstractTaskEvent event) {
		try {
			if (event instanceof StreamAnnounceEvent) {
				writer.publishEvent(event);
			} else if (event instanceof StreamAnnounceReplyEvent) {
				packetReader.publishEvent(event);
			}

		} catch (Exception e) {
		}
	}
}
