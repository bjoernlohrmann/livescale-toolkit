package de.tuberlin.cit.livescale.job.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import de.tuberlin.cit.livescale.job.util.merge.MergeGroup;
import eu.stratosphere.nephele.execution.Mapper;

public class MergeMapper implements Mapper<VideoFrame, VideoFrame> {

	private final Map<Long, MergeGroup> groupMap = new HashMap<Long, MergeGroup>();

	private final Queue<VideoFrame> outputCollector = new ArrayBlockingQueue<VideoFrame>(8192);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void map(final VideoFrame input) {

		final Long groupId = input.groupId;
		MergeGroup mergeGroup = this.groupMap.get(groupId);
		if (mergeGroup == null) {
			mergeGroup = new MergeGroup();
			this.groupMap.put(groupId, mergeGroup);
		}

		mergeGroup.addFrame(input);

		VideoFrame mergedFrame = mergeGroup.mergedFrameAvailable();
		while (mergedFrame != null) {
			this.outputCollector.add(mergedFrame);
			mergedFrame = mergeGroup.mergedFrameAvailable();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {

		this.groupMap.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Queue<VideoFrame> getOutputCollector() {

		return this.outputCollector;
	}
}
