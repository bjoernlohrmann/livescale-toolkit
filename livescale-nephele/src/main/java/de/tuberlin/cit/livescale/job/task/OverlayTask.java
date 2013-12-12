package de.tuberlin.cit.livescale.job.task;

import java.util.Queue;

import de.tuberlin.cit.livescale.job.event.StreamAnnounceEvent;
import de.tuberlin.cit.livescale.job.event.StreamAnnounceReplyEvent;
import de.tuberlin.cit.livescale.job.mapper.OverlayMapper;
import de.tuberlin.cit.livescale.job.record.VideoFrame;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.event.task.EventListener;
import eu.stratosphere.nephele.execution.Mapper;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractTask;

public class OverlayTask extends AbstractTask implements EventListener {

	private RecordReader<VideoFrame> reader;

	private RecordWriter<VideoFrame> writer;

	private Mapper<VideoFrame, VideoFrame> mapper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInputOutput() {
		this.reader = new RecordReader<VideoFrame>(this, VideoFrame.class);
		this.reader.subscribeToEvent(this, StreamAnnounceEvent.class);
		this.writer = new RecordWriter<VideoFrame>(this, VideoFrame.class);
		this.writer.subscribeToEvent(this, StreamAnnounceReplyEvent.class);
		this.mapper = new OverlayMapper(this.getTaskConfiguration());
		getEnvironment().registerMapper(this.mapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void invoke() throws Exception {

		final Queue<VideoFrame> outputCollector = this.mapper
				.getOutputCollector();

		try {
			// Consume the stream
			while (this.reader.hasNext()) {

				final VideoFrame frame = this.reader.next();

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

	@Override
	public void eventOccurred(AbstractTaskEvent event) {
		try {
			if (event instanceof StreamAnnounceEvent) {
				writer.publishEvent(event);
			} else if (event instanceof StreamAnnounceReplyEvent) {
				reader.publishEvent(event);
			}

		} catch (Exception e) {
		}
	}
}
