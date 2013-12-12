package de.tuberlin.cit.livescale.job.task;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.job.event.StreamAnnounceEvent;
import de.tuberlin.cit.livescale.job.event.StreamAnnounceReplyEvent;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.util.receiver.FileVideoReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.FlvOverTcpForwardingReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.MpegTsHttpServerReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.UdpForwardingReceiver;
import de.tuberlin.cit.livescale.job.util.receiver.VideoReceiver;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.event.task.EventListener;
import eu.stratosphere.nephele.io.RecordReader;
import eu.stratosphere.nephele.template.AbstractOutputTask;

public class VideoReceiverTask extends AbstractOutputTask implements
		EventListener {

	private static final Log LOG = LogFactory.getLog(VideoReceiverTask.class);

	public static final String BROADCAST_TRANSPORT = "BROADCAST_TRANSPORT";

	public static final String DEFAULT_BROADCAST_TRANSPORT = "http";

	private RecordReader<Packet> reader;

	private ConcurrentHashMap<Long, VideoReceiver> groupId2Receiver = new ConcurrentHashMap<Long, VideoReceiver>(
			16, 0.75f, 1);

	@Override
	public void registerInputOutput() {
		this.reader = new RecordReader<Packet>(this, Packet.class);
		this.reader.subscribeToEvent(this, StreamAnnounceEvent.class);
	}

	@Override
	public void invoke() throws Exception {
		try {
			while (reader.hasNext()) {
				Packet packet = reader.next();

				VideoReceiver receiver = groupId2Receiver.get(packet
						.getGroupId());
				if (receiver != null) {
					if (!packet.isEndOfStreamPacket()) {
						try {
							receiver.writePacket(packet);
						} catch (IOException e) {
							dropReceiver(receiver);
						}
					} else {
						dropReceiver(receiver);
					}
				} else {
					createVideoReceiver(packet.getGroupId(),
							UUID.randomUUID().toString()).writePacket(packet);
				}
			}
		} catch (InterruptedException e) {
		}

		shutdown();
	}

	private void dropReceiver(VideoReceiver receiver) {
		LOG.info("Closing receiver for stream-group " + receiver.getGroupId());
		receiver.closeSafely();
		groupId2Receiver.remove(receiver.getGroupId());
	}

	private void shutdown() {
		for (VideoReceiver receiver : groupId2Receiver.values()) {
			receiver.closeSafely();
		}
		groupId2Receiver.clear();
	}

	private VideoReceiver createVideoReceiver(long groupId,
			String receiveEndpointToken) throws Exception {

		VideoReceiver receiver = null;
		try {

			String broadcastTransport = getTaskConfiguration().getString(
					BROADCAST_TRANSPORT, DEFAULT_BROADCAST_TRANSPORT);

			if (broadcastTransport.startsWith("file://")) {
				receiver = new FileVideoReceiver(groupId, broadcastTransport
						+ groupId);
			} else if (broadcastTransport.startsWith("flv2tcp")) {
				receiver = new FlvOverTcpForwardingReceiver(groupId);
			} else if (broadcastTransport.startsWith("udp")) {
				receiver = new UdpForwardingReceiver(groupId,
						receiveEndpointToken);
			} else if (broadcastTransport.startsWith("http")) {
				receiver = new MpegTsHttpServerReceiver(groupId,
						receiveEndpointToken);
			} else {
				throw new RuntimeException("Unkown broadcast transport: "
						+ broadcastTransport.toString());
			}
			groupId2Receiver.put(groupId, receiver);
		} catch (IOException e) {
			LOG.error("Error when creating video receiver for stream-group "
					+ groupId, e);
		}

		return receiver;
	}

	@Override
	public void eventOccurred(AbstractTaskEvent event) {
		try {
			if (event instanceof StreamAnnounceEvent) {
				handleStreamAnnounceEvent((StreamAnnounceEvent) event);
			}
		} catch (Exception e) {
			LOG.error("Exception while handling event: "
					+ event.getClass().getSimpleName(), e);
		}
	}

	private void handleStreamAnnounceEvent(StreamAnnounceEvent event)
			throws Exception {
		int targetReceiver = (int) (event.getGroupId() % getCurrentNumberOfSubtasks());
		LOG.info("Received new stream announce event");
		if (targetReceiver == getIndexInSubtaskGroup()) {
			LOG.info("Handling new stream announce event");
			VideoReceiver receiver = createVideoReceiver(event.getGroupId(),
					event.getReceiveEndpointToken());
			StreamAnnounceReplyEvent reply = new StreamAnnounceReplyEvent(
					event.getStreamId(), event.getGroupId());
			reply.setSendEndpointToken(event.getSendEndpointToken());
			reply.setReceiveEndpointUrl(receiver.getReceiveEndpointURL());
			this.reader.publishEvent(reply);
		}
	}
}
