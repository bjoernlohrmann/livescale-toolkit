package de.tuberlin.cit.livescale.job.task.channelselectors;

import java.util.HashMap;

import de.tuberlin.cit.livescale.job.record.VideoFrame;
import eu.stratosphere.nephele.io.ChannelSelector;

public class GroupedVideoFrameChannelSelector implements ChannelSelector<VideoFrame> {

	private final int[] channelIds = new int[1];

	private HashMap<Long, Integer> groupId2ChanneldId = new HashMap<Long, Integer>();

	@Override
	public int[] selectChannels(VideoFrame frame, int numberOfOutputChannels) {
		Integer channelId = groupId2ChanneldId.get(frame.groupId);
		if (channelId == null) {
			channelId = (int) (frame.groupId % numberOfOutputChannels);
			groupId2ChanneldId.put(frame.groupId, channelId);
		}
		channelIds[0] = channelId;
		return channelIds;
	}

}
