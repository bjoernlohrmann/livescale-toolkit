package de.tuberlin.cit.livescale.job.task.channelselectors;

import java.util.HashMap;

import de.tuberlin.cit.livescale.job.record.Packet;
import eu.stratosphere.nephele.io.ChannelSelector;

public class GroupedPacketChannelSelector implements ChannelSelector<Packet> {

	private final int[] channelIds = new int[1];

	private HashMap<Long, Integer> groupId2ChanneldId = new HashMap<Long, Integer>();

	@Override
	public int[] selectChannels(Packet packet, int numberOfOutputChannels) {
		Integer channelId = groupId2ChanneldId.get(packet.getGroupId());
		if (channelId == null) {
			channelId = (int) (packet.getGroupId() % numberOfOutputChannels);
			groupId2ChanneldId.put(packet.getGroupId(), channelId);
		}
		channelIds[0] = channelId;
		return channelIds;
	}
}
