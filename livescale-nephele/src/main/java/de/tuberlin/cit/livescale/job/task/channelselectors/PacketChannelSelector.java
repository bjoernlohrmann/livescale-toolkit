package de.tuberlin.cit.livescale.job.task.channelselectors;

import java.util.HashMap;

import de.tuberlin.cit.livescale.job.record.Packet;
import eu.stratosphere.nephele.io.ChannelSelector;

public class PacketChannelSelector implements ChannelSelector<Packet> {

	private final int[] channelIds = new int[1];

	private HashMap<Long, Integer> streamId2ChanneldId = new HashMap<Long, Integer>();

	@Override
	public int[] selectChannels(Packet packet, int numberOfOutputChannels) {
		Integer channelId = streamId2ChanneldId.get(packet.getStreamId());
		if (channelId == null) {
			channelId = (int) (packet.getStreamId() % numberOfOutputChannels);
			streamId2ChanneldId.put(packet.getStreamId(), channelId);
		}
		channelIds[0] = channelId;
		return channelIds;
	}

	public void unregisterStreamId(long streamId) {
		streamId2ChanneldId.remove(streamId);
	}
}
