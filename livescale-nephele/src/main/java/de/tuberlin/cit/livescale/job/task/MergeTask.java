package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.mapper.MergeMapper;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.ChannelSelectorProvider;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.ChannelSelector;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public final class MergeTask extends AbstractTask {

	private RecordReader<VideoFrame> reader;

	private RecordWriter<VideoFrame> writer;

	private final Mapper<VideoFrame, VideoFrame> mapper = new MergeMapper();
	
	private ChannelSelector<VideoFrame> channelSelector;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.channelSelector = ChannelSelectorProvider
				.getVideoFrameChannelSelector(getTaskConfiguration());
		this.reader = new RecordReader<VideoFrame>(this, VideoFrame.class);
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
			while (this.reader.hasNext()) {

				final VideoFrame frame = this.reader.next();

				this.mapper.map(frame);

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
