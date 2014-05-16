package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.mapper.EncoderMapper;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.ChannelSelectorProvider;
import de.tuberlin.cit.livescale.job.util.encoder.VideoEncoder;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.ChannelSelector;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public final class EncoderTask extends AbstractTask {

	private RecordReader<VideoFrame> reader;

	private RecordWriter<Packet> packetWriter;

	private ChannelSelector<Packet> channelSelector;

	private Mapper<VideoFrame, Packet> mapper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.channelSelector = ChannelSelectorProvider
				.getPacketChannelSelector(getTaskConfiguration());
		this.reader = new RecordReader<VideoFrame>(this, VideoFrame.class);
		this.packetWriter = new RecordWriter<Packet>(this, Packet.class, this.channelSelector);
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
				
				VideoFrame frame = this.reader.next();
				if(frame.isDummyFrame()) {
					this.packetWriter.emit(new Packet());
					this.packetWriter.flush();
					continue;
				}

				this.mapper.map(frame);

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
}
