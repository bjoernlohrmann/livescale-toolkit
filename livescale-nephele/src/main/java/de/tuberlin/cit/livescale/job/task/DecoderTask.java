package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.mapper.DecoderMapper;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.ChannelSelectorProvider;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.ChannelSelector;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public final class DecoderTask extends AbstractTask {
	
	private RecordWriter<VideoFrame> writer;

	private RecordReader<Packet> packetReader;

	private ChannelSelector<VideoFrame> channelSelector;

	private final Mapper<Packet, VideoFrame> mapper = new DecoderMapper();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.channelSelector = ChannelSelectorProvider
				.getVideoFrameChannelSelector(getTaskConfiguration());
		this.packetReader = new RecordReader<Packet>(this, Packet.class);
		this.writer = new RecordWriter<VideoFrame>(this, VideoFrame.class, this.channelSelector);
		getEnvironment().registerMapper(this.mapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke() throws Exception {

		final Queue<VideoFrame> outputCollector = this.mapper.getOutputCollector();

		try {
			while (this.packetReader.hasNext()) {
				Packet packet = this.packetReader.next();
				if(packet.isDummyPacket()) {
					this.writer.emit(new VideoFrame());
					this.writer.flush();
					continue;
				}
				this.mapper.map(packet);

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
}
