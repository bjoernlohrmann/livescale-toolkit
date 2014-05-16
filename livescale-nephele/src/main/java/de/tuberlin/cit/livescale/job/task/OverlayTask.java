package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.mapper.OverlayMapper;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.task.channelselectors.ChannelSelectorProvider;
import eu.stratosphere.nephele.io.ChannelSelector;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public class OverlayTask extends AbstractTask {

	private RecordReader<VideoFrame> reader;

	private RecordWriter<VideoFrame> writer;

	private OverlayMapper mapper;
	
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
		this.mapper = new OverlayMapper(this.getTaskConfiguration());
		getEnvironment().registerMapper(this.mapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke() throws Exception {
		
		this.mapper.startOverlays();
		
		final Queue<VideoFrame> outputCollector = this.mapper
				.getOutputCollector();

		try {
			// Consume the stream
			while (this.reader.hasNext()) {

				final VideoFrame frame = this.reader.next();
				
				if(frame.isDummyFrame()) {
					this.writer.emit(new VideoFrame());
					this.writer.flush();
					continue;
				}

				this.mapper.map(frame);

				boolean shouldFlush = false;
				while (!outputCollector.isEmpty()) {
					VideoFrame frameToEmit = outputCollector.poll();
					this.writer.emit(frameToEmit);
					shouldFlush = shouldFlush
							|| frameToEmit.isEndOfStreamFrame();
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
