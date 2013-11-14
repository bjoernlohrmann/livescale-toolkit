package de.tuberlin.cit.livescale.job.task;

import java.io.IOException;
import java.net.URI;
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
import eu.stratosphere.nephele.template.AbstractFileOutputTask;

public class VideoReceiverTask extends AbstractFileOutputTask implements EventListener {

	private static final Log LOG = LogFactory.getLog(VideoReceiverTask.class);

	private RecordReader<Packet> reader;

	private ConcurrentHashMap<Long, VideoReceiver> groupId2Receiver = new ConcurrentHashMap<Long, VideoReceiver>(16,
		0.75f, 1);

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

				VideoReceiver receiver = groupId2Receiver.get(packet.getGroupId());
				if (receiver != null) {
					if (!packet.isEndOfStreamPacket()) {
						try {
							receiver.writePacket(packet);
						} catch (IOException e) {
							LOG.error("Error when writing packet of stream-group " + receiver.getGroupId(), e);
							dropReceiver(receiver);
						}
					} else {
						dropReceiver(receiver);
					}
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

	private VideoReceiver createVideoReceiver(long groupId, String receiveEndpointToken) throws Exception {

		VideoReceiver receiver = null;
		try {
			URI uri = getFileOutputPath().toUri();
			if (uri.getScheme().startsWith("file")) {
				receiver = new FileVideoReceiver(groupId, getFileOutputPath().toUri().getPath() + groupId);
			} else if (uri.getScheme().startsWith("flv2tcp")) {
				receiver = new FlvOverTcpForwardingReceiver(groupId);
			} else if (uri.getScheme().startsWith("udp")) {
				receiver = new UdpForwardingReceiver(groupId, receiveEndpointToken);
			} else if (uri.getScheme().startsWith("http")) {
				receiver = new MpegTsHttpServerReceiver(groupId, receiveEndpointToken);
			} else {
				throw new RuntimeException("Unkown protocol specifier in URI " + uri.toString());
			}
			groupId2Receiver.put(groupId, receiver);
		} catch (IOException e) {
			LOG.error("Error when creating video receiver for stream-group " + groupId, e);
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
			LOG.error("Exception while handling event: " + event.getClass().getSimpleName(), e);
		}
	}

	private void handleStreamAnnounceEvent(StreamAnnounceEvent event) throws Exception {
		int targetReceiver = (int) (event.getGroupId() % getCurrentNumberOfSubtasks());
		LOG.info("Received new stream announce event");
		if (targetReceiver == getIndexInSubtaskGroup()) {
			LOG.info("Handling new stream announce event");
			VideoReceiver receiver = createVideoReceiver(event.getGroupId(), event.getReceiveEndpointToken());
			StreamAnnounceReplyEvent reply = new StreamAnnounceReplyEvent(event.getStreamId(), event.getGroupId());
			reply.setSendEndpointToken(event.getSendEndpointToken());
			reply.setReceiveEndpointUrl(receiver.getReceiveEndpointURL());
			this.reader.publishEvent(reply);
		}
	}
}
