package de.tuberlin.cit.livescale.job.task;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.job.event.StreamAnnounceEvent;
import de.tuberlin.cit.livescale.job.event.StreamAnnounceReplyEvent;
import de.tuberlin.cit.livescale.job.record.Packet;
import de.tuberlin.cit.livescale.job.task.channelselectors.PacketChannelSelector;
import de.tuberlin.cit.livescale.job.util.source.IdGenerator;
import de.tuberlin.cit.livescale.job.util.source.PacketFactory;
import de.tuberlin.cit.livescale.job.util.source.Stream;
import de.tuberlin.cit.livescale.job.util.source.StreamManager;
import de.tuberlin.cit.livescale.messaging.AbstractMessage;
import de.tuberlin.cit.livescale.messaging.Message;
import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.MessageListener;
import de.tuberlin.cit.livescale.messaging.RequestMessage;
import de.tuberlin.cit.livescale.messaging.endpoints.AMQPEndpoint;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamClose;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamConfirm;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamStatus;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverNewStream;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverRequestStreamStatus;
import eu.stratosphere.nephele.event.task.AbstractTaskEvent;
import eu.stratosphere.nephele.event.task.EventListener;
import eu.stratosphere.nephele.io.RecordWriter;
import eu.stratosphere.nephele.template.AbstractGenericInputTask;
//import de.tuberlin.cit.livestream.messaging.Message;
//import de.tuberlin.cit.livestream.messaging.StreamServerCommunicationManager;
//import de.tuberlin.cit.livestream.messaging.streamserver.NewStreamMessage;
//import de.tuberlin.cit.livestream.messaging.streamserver.RequestStreamStatusMessage;
//import de.tuberlin.cit.livestream.messaging.streamserver.StreamServerMessageHandler;

public class PacketizedStreamHandler extends AbstractGenericInputTask implements
		EventListener {

	private static final Log LOG = LogFactory
			.getLog(PacketizedStreamHandler.class);
	
	private final static String ROUTING_KEY_DISPATCHER = "dispatcher";
	
	public static final String STREAM_HANDLER_SERVER_PORT = "packetizedStreamHandlerServerPort";

	public static final String MESSAGING_BROKER_HOST = "packetizedStreamHandlerMessagingBrokerHost";

	public static final String MESSAGING_BROKER_PORT = "packetizedStreamHandlerMessagingBrokerPort";
	
	private static final String CIT_STREAM_EXCHANGE = "cit_stream_exchange";
	
	private RecordWriter<Packet> out = null;

	private int connectedClients = 0;

	private ByteBuffer readBuffer = ByteBuffer.allocateDirect(4096);

	private ServerSocketChannel serverChannel;

	private PacketChannelSelector channelSelector = new PacketChannelSelector();

	private int serverPort;

	private MessageCenter messageCenter;
	
	private StreamManager streamManager;

	private LinkedBlockingQueue<Message> messageQueue;

	private LinkedBlockingQueue<AbstractTaskEvent> taskEventQueue;
	
	private HashMap<String, TokenInfo> tokenInfos = new HashMap<String, TokenInfo>();

	private class TokenInfo {
		long timestamp; // not used at the moment ... as seen in the original
		RequestMessage message;
		
		public TokenInfo(RequestMessage message) {
			this.message = message;
			this.timestamp = System.currentTimeMillis();
		}
	}
	
	@Override
	public void registerInputOutput() {
		this.out = new RecordWriter<Packet>(this, Packet.class,
				this.channelSelector);
		this.out.subscribeToEvent(this, StreamAnnounceReplyEvent.class);
	}

	private ServerSocketChannel createServerSocket(int serverPort)
			throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		// InetAddress lh = InetAddress.getByName(determineHostAddress());
		InetSocketAddress isa = new InetSocketAddress(serverPort);
		serverChannel.socket().bind(isa);
		return serverChannel;
	}

	@Override
	public void invoke() throws Exception {
		this.serverPort = this.getTaskConfiguration().getInteger(
				STREAM_HANDLER_SERVER_PORT, 9000);
		this.streamManager = new StreamManager(new IdGenerator(
				this.getMaximumNumberOfSubtasks(),
				this.getIndexInSubtaskGroup(), 10000000), this.serverPort);

		this.setupMessaging();

		this.serverChannel = this.createServerSocket(this.serverPort);

		Selector selector = SelectorProvider.provider().openSelector();
		this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		try {
			while (!Thread.interrupted()) {
				this.handlePendingMessages();
				this.handlePendingTaskEvents();

				if (selector.select(100) == 0) {
					if (!Thread.interrupted()) {
						continue;
					}
					break;
				}

				Iterator<SelectionKey> i = selector.selectedKeys().iterator();

				while (i.hasNext()) {

					SelectionKey key = i.next();
					i.remove();

					if (!key.isValid()) {
						this.closeStreamSafely(key);
					} else if (key.isAcceptable()) {
						this.acceptConnection(key, selector);
					} else if (key.isReadable()) {
						this.emitNextPackets(key);
					}
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}

		this.shutdown();
	}

	private void setupMessaging() {
//		String brokerHost = this.getTaskConfiguration().getString(
//				MESSAGING_BROKER_HOST, "localhost");
//		int brokerPort = this.getTaskConfiguration().getInteger(
//				MESSAGING_BROKER_PORT, 5672);
		this.messageQueue = new LinkedBlockingQueue<Message>();
//		this.messagingManager = new StreamServerCommunicationManager(
//				brokerHost, brokerPort, "#", this);
		try {
			this.messageCenter = new MessageCenter(false);
			// one for al
			this.messageCenter.addMessageListener(StreamserverNewStream.class, new MessageListener<StreamserverNewStream>() {
				@Override
				public void handleMessageReceived(StreamserverNewStream message) {
					LOG.debug(String.format("Received message: %s", message.getClass().getSimpleName()));
					PacketizedStreamHandler.this.messageQueue.add(message);

				}
			});
			this.messageCenter.addMessageListener(StreamserverRequestStreamStatus.class, new MessageListener<StreamserverRequestStreamStatus>() {
				@Override
				public void handleMessageReceived(StreamserverRequestStreamStatus message) {
					LOG.debug(String.format("Received message: %s", message.getClass().getSimpleName()));
					PacketizedStreamHandler.this.messageQueue.add(message);
				}
			});
//			this.messageCenter.addMessageListener(AbstractMessage.class, new MessageListener<AbstractMessage>() {
//				@Override
//				public void handleMessageReceived(AbstractMessage message) {
//					System.out.println("narf");
//					LOG.debug(String.format("Received message: %s", message.getClass().getSimpleName()));
//					PacketizedStreamHandler.this.messageQueue.add(message);
//				}
//			});
			this.messageCenter.startAllEndpoints();
			LOG.debug("Messaging set up");
		} catch (IOException e) {
			LOG.fatal("Could not start Message center");
			shutdown();
			System.exit(1);
		}
		
		this.taskEventQueue = new LinkedBlockingQueue<AbstractTaskEvent>();
	}

	private void shutdown() {
		// closes all client connections and drops all stream meta information
		for (Stream stream : this.streamManager.getAllActiveStreams()) {
			this.closeStreamSafely(stream);
		}

		// close connection to messaging broker
		try {
			this.serverChannel.close();
			this.messageCenter.shutdownAllEndpoints();
		} catch (IOException e) {
		}

		// clear large memory structures
		this.messageQueue.clear();
		this.taskEventQueue.clear();
	}

	private void acceptConnection(SelectionKey serverChannelKey,
			Selector selector) throws IOException {

		ServerSocketChannel channel = (ServerSocketChannel) serverChannelKey
				.channel();
		SocketChannel clientChannel = channel.accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(selector, SelectionKey.OP_READ);

		this.connectedClients++;
		LOG.info("Connected clients+1: " + this.connectedClients);
	}

	// private long createStreamId(SocketChannel clientChannel) {
	// InetSocketAddress socketAddress = ((InetSocketAddress)
	// clientChannel.socket().getRemoteSocketAddress());
	// long address = socketAddress.getAddress().hashCode();
	// long port = socketAddress.getPort();
	// return (address << 16) | port;
	// }

	private int emitNextPackets(SelectionKey key) throws IOException,
			InterruptedException {
		boolean success = this.readFromChannelIntoReadBuffer(key);

		int emitted = 0;
		if (success) {
			List<Packet> packets = this.fillPacketsUntilReadBufferEmpty(key);
			for (Packet packet : packets) {
				this.out.emit(packet);
				emitted++;
			}
		}

		return emitted;
	}

	private boolean readFromChannelIntoReadBuffer(SelectionKey key) {
		boolean success = false;

		SocketChannel socketChannel = (SocketChannel) key.channel();

		try {
			this.readBuffer.clear();
			int read = socketChannel.read(this.readBuffer);
			this.readBuffer.flip();

			// -1 means we have reached end of stream
			if (read == -1) {
				this.closeStreamSafely(key);
			} else {
				success = true;
			}
		} catch (IOException e) {
			this.closeStreamSafely(key);
		}
		return success;
	}

	private LinkedList<Packet> completePackets = new LinkedList<Packet>();

	private LinkedList<Packet> fillPacketsUntilReadBufferEmpty(SelectionKey key)
			throws IOException {

		this.completePackets.clear();

		Stream stream = this.streamManager.getStream(key);

		while (this.readBuffer.hasRemaining()) {
			if (stream != null) {
				Packet completePacket = stream.getStreamPacketFactory()
						.processDataInBuffer(this.readBuffer);
				if (completePacket != null) {
					this.completePackets.add(completePacket);
				}
			} else {
				stream = this.authenticateClient(key);
				if (stream == null) {
					break;
				}
			}
		}

		return this.completePackets;
	}

	private Stream authenticateClient(SelectionKey key) throws IOException {

		PacketFactory tmpPacketFactory = new PacketFactory(0, 0);
		Packet authTokenPacket = tmpPacketFactory
				.processDataInBuffer(this.readBuffer);
		if (authTokenPacket != null) {
			String sendEndpointToken = new String(authTokenPacket.getData(),
					"US-ASCII");

			Stream stream = this.streamManager.getStream(sendEndpointToken);
			if (stream != null) {
				stream.setSelectionKey(key);
				stream.setStreamPacketFactory(new PacketFactory(stream
						.getStreamId(), stream.getGroupId()));
				LOG.info(String.format(
						"Client authentication success with send token %s",
						sendEndpointToken));
				return stream;
			}
			LOG.info(String.format(
					"Client authentication failure for send token %s",
					sendEndpointToken));
		} else {
			LOG.info("Client authentication failure");
		}

		this.closeStreamSafely(key);
		this.readBuffer.clear();
		return null;
	}

	private void closeStreamSafely(SelectionKey key) {
		Stream stream = this.streamManager.getStream(key);
		if (stream != null) {
			this.closeStreamSafely(stream);
		} else {
			this.closeConnectionSafely(key);
		}
	}

	private void closeConnectionSafely(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		key.cancel();
		try {
			socketChannel.close();
		} catch (IOException e) {
		}
		this.connectedClients--;
		LOG.info("Connected clients-1: " + this.connectedClients);
	}

	private void closeStreamSafely(Stream stream) {
		// closes connection and unregisters stream at stream manager
		this.closeConnectionSafely(stream.getSelectionKey());
		stream.release();
		// this is not needed here ...  do we actually need this?!?
		// TokenInfo tokenInfo = this.tokenInfos.remove(stream.getSendEndpointToken());
		DispatcherStreamClose msg = new DispatcherStreamClose();
		msg.setSendToken(stream.getSendEndpointToken());
		msg.setReceiveToken(stream.getReceiveEndpointToken());
		try {
			LOG.debug("Sending DispatcherStreamClose");
			URI uri = new AMQPEndpoint.URIBuilder().
					exchangeName(CIT_STREAM_EXCHANGE).
					routingKey(new String[] {"broadcast", ROUTING_KEY_DISPATCHER}).
					build();
			this.messageCenter.send(msg, uri);
		} catch (URISyntaxException e1) {
			LOG.fatal("Unable to encode Dispatcher URI. This should never happen", e1);
		}
		try {
			this.out.emit(this.createEndOfStreamPacket(stream));
			this.out.flush();
		} catch (Exception e) {
		}
		this.channelSelector.unregisterStreamId(stream.getStreamId());
	}

	private Packet createEndOfStreamPacket(Stream stream) {
		LOG.info(String.format("Creating end of stream packet for stream %d",
				stream.getStreamId()));
		Packet endOfStreamPacket = new Packet(stream.getStreamId(),
				stream.getGroupId(), 0, null);
		endOfStreamPacket.markAsEndOfStreamPacket();
		return endOfStreamPacket;
	}

	private void handlePendingMessages() throws Exception {
		while (!this.messageQueue.isEmpty()) {
			Message msg = this.messageQueue.remove();
			LOG.debug(String.format("New Message: %s", msg.getClass().getSimpleName()));
			if(msg instanceof StreamserverRequestStreamStatus) {
				this.handleRequestStreamStatusMessage(
						(StreamserverRequestStreamStatus) msg
				);
			}
			else if(msg instanceof StreamserverNewStream) {
				this.handleNewStreamMessage(
						(StreamserverNewStream) msg
				);
			}
		}
	}

	private void handleNewStreamMessage(StreamserverNewStream msg) throws Exception {
		LOG.debug("Handling NewStreamMessage");
		this.tokenInfos.put(msg.getSendEndpointToken(), new TokenInfo(msg));
		
		Stream stream = this.streamManager.createInitialStream(msg);
		
		StreamAnnounceEvent streamAnnounce = new StreamAnnounceEvent(
				stream.getStreamId(), stream.getGroupId());
		streamAnnounce.setSendEndpointToken(stream.getSendEndpointToken());
		streamAnnounce
				.setReceiveEndpointToken(stream.getReceiveEndpointToken());
		LOG.debug("Publishing streamAnnounce");
		this.out.publishEvent(streamAnnounce);
	}
	
	private void handleRequestStreamStatusMessage(StreamserverRequestStreamStatus msg) {
		LOG.debug("Handling StreamserverRequestStreamStatus");
		this.tokenInfos.put(msg.getSendEndpointToken(), new TokenInfo(msg));
		Stream stream = this.streamManager
				.getStream(msg.getSendEndpointToken());
		boolean isActive = stream != null && stream.isActive();
		// removed addReplyDetailsToBodyMap with tokenInfos
		DispatcherStreamStatus status = (DispatcherStreamStatus) msg.getResponseMessage();
		status.setActive(isActive);
		if(stream != null) {
			status.setSendEndpointToken(stream.getSendEndpointToken());
			status.setReceiveEndpointToken(stream.getReceiveEndpointToken());
		}
		else {
			status.setSendEndpointToken("");
			status.setReceiveEndpointToken("");
		}
		LOG.debug(String.format("Returning status isActive: %b", isActive));
		this.messageCenter.sendResponse(status);
	}

	private void handlePendingTaskEvents() {
		while (!this.taskEventQueue.isEmpty()) {
			AbstractTaskEvent event = this.taskEventQueue.remove();

			try {
				if (event instanceof StreamAnnounceReplyEvent) {
					this.handleStreamAnnounceReplyEvent((StreamAnnounceReplyEvent) event);
				}
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}

	@Override
	public void eventOccurred(AbstractTaskEvent event) {
		this.taskEventQueue.add(event);
	}
	
	private void handleStreamAnnounceReplyEvent(StreamAnnounceReplyEvent event)
			throws MalformedURLException {
		LOG.debug("Handling StreamAnnounceReplyEvent");
		Stream stream = this.streamManager.getStream(event
				.getSendEndpointToken());
		URL url = new URL(event.getReceiveEndpointUrl());
		stream.setReceiveEndpointHost(url.getHost());
		stream.setReceiveEndpointPort(url.getPort());
		// removed addReplayDetailsToBodyMap (is handled by the Center)
		StreamserverNewStream req = (StreamserverNewStream) this.tokenInfos.get(stream.getSendEndpointToken()).message;
		DispatcherStreamConfirm dsc = (DispatcherStreamConfirm) req.getResponseMessage();
		dsc.setUsername(req.getUsername());
		dsc.setSendEndpointToken(stream.getSendEndpointToken());
		dsc.setSendEndpointAddress(stream.getSendEndpointHost());
		dsc.setSendEndpointPort(stream.getSendEndpointPort());
		dsc.setReceiveEndpointToken(stream.getReceiveEndpointToken());
		dsc.setReceiveEndpointAddress(stream.getReceiveEndpointHost());
		dsc.setReceiveEndpointPort(stream.getReceiveEndpointPort());
		LOG.debug("Sending DispatcherStreamConfirm Message");
		this.messageCenter.sendResponse(dsc);
	}
}
