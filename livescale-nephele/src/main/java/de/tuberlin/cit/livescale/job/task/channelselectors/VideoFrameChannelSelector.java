package de.tuberlin.cit.livescale.job.task.channelselectors;

import java.util.HashMap;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import eu.stratosphere.nephele.io.ChannelSelector;

public class VideoFrameChannelSelector implements ChannelSelector<VideoFrame> {

	private final int[] channelIds = new int[1];

	private HashMap<Long, Integer> streamId2ChanneldId = new HashMap<Long, Integer>();

	private int lastUsedChannelId = -1;

	private int noOfOutputChannels = 1;

	@Override
	public int[] selectChannels(VideoFrame frame, int numberOfOutputChannels) {
		noOfOutputChannels = numberOfOutputChannels;
		channelIds[0] = streamId2ChanneldId.get(frame.streamId);
		return channelIds;
	}

	/**
	 * This method is safe for re-registration. Re-registering a streamId
	 * will not change the mappen of streamId to output channel.
	 * 
	 * @param streamId
	 */
	public void registerStreamId(long streamId) {
		if (!streamId2ChanneldId.containsKey(streamId)) {
			lastUsedChannelId = (lastUsedChannelId + 1) % noOfOutputChannels;
			streamId2ChanneldId.put(streamId, lastUsedChannelId);
		}
	}

	public void unregisterStreamId(long streamId) {
		streamId2ChanneldId.remove(streamId);
	}
}
