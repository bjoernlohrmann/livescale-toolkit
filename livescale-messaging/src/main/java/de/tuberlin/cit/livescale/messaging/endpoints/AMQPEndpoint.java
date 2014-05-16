/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package de.tuberlin.cit.livescale.messaging.endpoints;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import de.tuberlin.cit.livescale.messaging.MessageFactory;
import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * Represents an AMQPEndpoint.
 * 
 * Configuration keys:
 * <ul>
 * <li>{@link String} <code>brokerAddress</code> (required)</li>
 * <li>{@link String} <code>exchangeName</code> (required)</li>
 * <li>{@link Integer} <code>brokerPort</code> (optional)</li>
 * <li>{@link String} <code>routingKey</code> (optional, default "#")</li>
 * </ul>
 * 
 * A note on ReponseURIs. You'll need not provide a responseURI because frankly
 * you won't know what the "private" queue instanceName is.
 * ResponseURIs are provided by {@link AMQPEndpoint}
 * 
 * URI schema: amqp://[nameofthisinstance]/routingkey[?exchange=string]
 * 
 * @author Bernd Louis
 */
public class AMQPEndpoint extends AbstractMessageEndpoint  {
	private static final String QUERY_KEY_EXCHANGE = "exchange";

	private static final Log LOG = LogFactory.getLog(AMQPEndpoint.class);

	protected static final String SCHEME = "amqp";

	// CONFIG KEY constants
	protected static final String CONFIG_KEY_ROUTING_KEY = "routingKey";
	protected static final String CONFIG_KEY_EXCHANGE_NAME = "exchangeName";
	protected static final String CONFIG_KEY_BROKER_PORT = "brokerPort";
	protected static final String CONFIG_KEY_BROKER_ADDRESS = "brokerAddress";
	protected static final String CONFIG_KEY_LISTEN_FOR_TASKS = "listenForTasks";
	protected static final String CONFIG_KEY_LISTEN_FOR_BROADCASTS = "listenForBroadcasts";
	private static final String[] requiredConfigFields = {
			CONFIG_KEY_BROKER_ADDRESS, CONFIG_KEY_EXCHANGE_NAME };
	
	// config options
	private String brokerAddress;
	private Integer brokerPort; // non primitive
	private String routingKey;
	private String exchangeName;
	
	private boolean listenForBroadcasts = false;
	private boolean listenForTasks = false;

	// AMQP stuff
	private static final String QUEUE_NAME_TASKS = "cit_stream_tasks_queue_"; 
	private static final String ROUTING_KEY_BROADCAST = "broadcast";
	private static final String ROUTING_KEY_TASK = "task";
	private static final String ROUTING_KEY_DELIMITER = ".";
	private static final String ROUTING_KEY_WILDCARD = "#";

	// "actual state"
	private ConnectionFactory connectionFactory = new ConnectionFactory();
	private Connection connection;
	private Channel channel;
	private String exclusiveQueueName;
	private QueueingConsumer consumer;
	private UUID uuid;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#configure
	 * (java.util.Map)
	 */
	@Override
	public void configure(Map<String, String> properties) {
		this.assertRequiredConfigKeysPresent(properties);
		this.brokerAddress = properties.get(CONFIG_KEY_BROKER_ADDRESS);
		this.exchangeName = properties.get(CONFIG_KEY_EXCHANGE_NAME);
		try {
			this.brokerPort = Integer.parseInt(properties
					.get(CONFIG_KEY_BROKER_PORT));
		} catch (NumberFormatException e) {
			this.brokerPort = null;
			LOG.debug("Using default port");
		}
		this.routingKey = properties.get(CONFIG_KEY_ROUTING_KEY);
		if (this.routingKey == null) {
			this.routingKey = "#";
			LOG.debug(String
					.format("Using default routingKey", this.routingKey));
		}
		if (properties.containsKey(CONFIG_KEY_LISTEN_FOR_BROADCASTS)) {
			this.listenForBroadcasts = Boolean.valueOf(properties
					.get(CONFIG_KEY_LISTEN_FOR_BROADCASTS));
		}
		if (properties.containsKey(CONFIG_KEY_LISTEN_FOR_TASKS)) {
			this.listenForTasks = Boolean.valueOf(properties
					.get(CONFIG_KEY_LISTEN_FOR_TASKS));
		}
	}
	
	/**
	 * Will throw {@link IllegalArgumentException} if not all required config
	 * keys are present.
	 * 
	 * @param properties
	 */
	private void assertRequiredConfigKeysPresent(Map<String, String> properties) {
		for (String k : requiredConfigFields) {
			if (!properties.containsKey(k)) {
				throw new IllegalArgumentException(String.format(
						"Required configuration key missing: %s", k));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performSend(de.tuberlin.cit.livescale.messaging.MessageManifest)
	 */
	@Override
	protected void performSend(MessageManifest manifest) {
		URI targetURI = manifest.getTargetURI();
		// URI response = manifest.getResponseURI();
		BasicProperties bp = null;
		URIParser target = new URIParser(targetURI);
		
		// figure out our target
		String routingKey = target.getRoutingKey();
		String targetExchange = target.getExchangeName();
		// do we need a response? 
		if(manifest.isResponseRequired()) { 
			bp = new BasicProperties.Builder().
					replyTo(this.exclusiveQueueName).
					appId(this.uuid.toString()).
					build();
		}
		else {
			bp = new BasicProperties.Builder().appId(this.uuid.toString()).build();
		}
		try {
			this.channel.basicPublish(targetExchange, routingKey, 
					bp, MessageFactory.encode(manifest));
		} catch (IOException e) {
			notifyListenersDeliveryEvent(new ServiceUnavailableFailure(manifest));
		}
	}
	
	/**
	 * Connects to the broker
	 */
	private void connect() throws IOException {
		this.connectionFactory.setHost(this.brokerAddress);
		if (this.brokerPort != null) {
			this.connectionFactory.setPort(this.brokerPort);
		}
		this.connection = this.connectionFactory.newConnection();
		this.channel = this.connection.createChannel();
		this.channel.basicQos(1);
		// create the exchangeName 
		this.channel.exchangeDeclare(this.exchangeName, "topic", false, true,
				null);
		// my queue 
		this.exclusiveQueueName = this.channel.queueDeclare().getQueue();
		if (this.listenForBroadcasts) { // listen for "broadcast"
			this.channel.queueBind(this.exclusiveQueueName, this.exchangeName,
					ROUTING_KEY_BROADCAST);
		}
		// listen for "broadcast.[this.routingKey] e.g.
		// "broadcast.dispatcher" 
		this.channel.queueBind( 
				this.exclusiveQueueName,
				this.exchangeName,
				new StringBuilder(ROUTING_KEY_BROADCAST)
						.append(ROUTING_KEY_DELIMITER)
						.append(this.routingKey).toString());
		this.consumer = new QueueingConsumer(this.channel);
		LOG.info("Bound to " + new StringBuilder(ROUTING_KEY_BROADCAST)
		.append(ROUTING_KEY_DELIMITER)
		.append(this.routingKey).toString());
		
		// declare a new queue with the instanceName "cit_stream_tasks_queue_[routingKey]"
		// for instance: "cit_stream_tasks_queue_dispatcher"
		// then bind this queue to this.exchangeName
		// "task.[routingKey]" e.g. "task.dispatcher"
		if (this.listenForTasks) {
			String queueNameTasks = new StringBuilder(QUEUE_NAME_TASKS)
					.append(this.routingKey).toString();
			this.channel.queueDeclare(queueNameTasks, false, false, true,
					null);
			String listenRoutingKey = new StringBuilder(ROUTING_KEY_TASK)
					.append(ROUTING_KEY_DELIMITER).append(this.routingKey)
					.toString();
			this.channel.queueBind(queueNameTasks, this.exchangeName,
					listenRoutingKey);
		}
		this.uuid = UUID.randomUUID();
	}
	
	/**
	 * Sets up a consumer that passes received messages to the
	 * {@link MessageEndpointListener}s.
	 */
	private void setUpConsumer() throws IOException {
		DefaultConsumer consumer = new DefaultConsumer(this.channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope,
					BasicProperties properties, byte[] body) throws IOException {
				long deliveryTag = envelope.getDeliveryTag();
				super.getChannel().basicAck(deliveryTag, false);
				if(!properties.getAppId().equals(AMQPEndpoint.this.uuid.toString())) {
					MessageManifest mm = MessageFactory.decode(body);
					try {
						URI responseURI = new URIBuilder().
								routingKey(properties.getReplyTo()).
								instanceName(getName()).build();
						mm.setResponseURI(responseURI);
					} catch (URISyntaxException e) { // this should never happen!
						LOG.warn("Unable to create response URI for incoming message", e);
					}
					notifyMessageEndpointListeners(mm);
				}
			}
		};
		if(this.listenForTasks) {
			String queueName = QUEUE_NAME_TASKS + this.routingKey;
			this.channel.basicConsume(queueName, false, consumer);
		}
		this.channel.basicConsume(this.exclusiveQueueName, false, consumer);
		
		// listening to the remote shutdown
		ShutdownListener shutdownListener = new ShutdownListener() {
			@Override
			public void shutdownCompleted(ShutdownSignalException cause) {
				shutdown();
			}
		};
		this.channel.addShutdownListener(shutdownListener);
//		if (this.channel.isOpen()) {
//			handler.handleConnectionEvent(true);
//		}
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#getScheme()
	 */
	@Override
	protected String getScheme() {
		return SCHEME;
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#start()
	 */
	@Override
	public void performStart() throws IOException {
		if(this.brokerAddress == null || this.routingKey == null) {
			throw new IllegalStateException(String.format("Cannot start %s, is not properly configured. ", this.getClass().getSimpleName()));
		}
		connect();
		setUpConsumer();
	}
	
	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#shutdown()
	 */
	@Override
	public void performShutdown() {
		try {
			// do something with the consumer?
			if(this.channel != null && this.channel.isOpen()) {
				this.channel.close();
			}
			this.channel = null;
			if(this.connection != null && this.connection.isOpen()) {
				this.connection.close();
			}
			this.connection = null;
		} catch (IOException e) {
			LOG.warn("Errors while disconnecting from the broker.", e);
		}
	}
	
	/**
	 * Builder class to create URIs compatible with an {@link AMQPEndpoint}
	 * 
	 * @author Bernd Louis
	 */
	public static class URIBuilder {
		private String routingKey = "";
		private String instanceName = "";
		private String exchangeName = "";
		
		public URIBuilder routingKey(String routingKey) { this.routingKey = routingKey; return this;}
		public URIBuilder routingKey(String[] routingKey) {
			// this.routingKey =
			this.routingKey = routingKey[0];
			for(int i = 1; i < routingKey.length; i++) {
				this.routingKey += "." + routingKey[i];
			}
			return this;
		}
		public URIBuilder instanceName(String instanceName) {this.instanceName = instanceName; return this;}
		public URIBuilder exchangeName(String exchangeName) { this.exchangeName = exchangeName; return this;}
		
		public URI build() throws URISyntaxException {
			String path = "/" + this.routingKey;
			String query = "";
			String authority = this.instanceName;
			if(this.exchangeName != null && !(this.exchangeName.isEmpty())) {
				query = "exchange=" + this.exchangeName;
			}
			return new URI(AMQPEndpoint.SCHEME, authority, path, query, "");
		}
		
	}
	
	/**
	 * This class is used to create a valid {@link Properties} instance.
	 * 
	 * {@link Properties} implements {@link Map<Object, Object>} 
	 * 
	 * @author Bernd Louis
	 */
	public static class ConfigBuilder {
		private String routingKey = null;
		private String exchangeName = null;
		private int brokerPort = 0;
		private String brokerAddress = null;
		private boolean listenForTasks = false;
		private boolean listenForBroadcasts = false;
		
		public ConfigBuilder routingKey(String routingKey) { this.routingKey = routingKey; return this;}
		public ConfigBuilder exchangeName(String exchangeName) { this.exchangeName = exchangeName; return this;}
		public ConfigBuilder brokerPort(int brokerPort) { this.brokerPort = brokerPort; return this;}
		public ConfigBuilder brokerAddress(String brokerAddress) {this.brokerAddress = brokerAddress; return this; }
		public ConfigBuilder listenForTasks(boolean listenForTasks) {this.listenForTasks = listenForTasks; return this;}
		public ConfigBuilder listenForBroadcasts(boolean listenForBroadcasts) {this.listenForBroadcasts = listenForBroadcasts; return this;}
		
		public Properties build(String domain) {
			Properties map = new Properties();
			if(	this.brokerAddress == null || 
				this.exchangeName == null	) {
					throw new IllegalArgumentException(String.format("%s configuration is invalid", AMQPEndpoint.class.getSimpleName()));
			}
			String domainPrefix = "";
			if(domain != null && !(domain.isEmpty())) {
				domainPrefix = domain + ".";
			}
			if(this.routingKey != null) {
				map.put(domainPrefix + CONFIG_KEY_ROUTING_KEY, this.routingKey);				
			}
			map.put(domainPrefix + "class", AMQPEndpoint.class.getName());
			map.put(domainPrefix + CONFIG_KEY_EXCHANGE_NAME, this.exchangeName);
			map.put(domainPrefix + CONFIG_KEY_BROKER_ADDRESS, this.brokerAddress);
			map.put(domainPrefix + CONFIG_KEY_BROKER_PORT, String.valueOf(this.brokerPort));
			map.put(domainPrefix + CONFIG_KEY_LISTEN_FOR_TASKS, String.valueOf(this.listenForTasks));
			map.put(domainPrefix + CONFIG_KEY_LISTEN_FOR_BROADCASTS, String.valueOf(this.listenForBroadcasts));
			return map;
		}
		
		public Properties build() {
			return build(null);
		}
	}
	
	/**
	 * Helper class to parse the URI for the actual stuff
	 * 
	 * This is protected so we have some test on this
	 * 
	 * @author Bernd Louis
	 */
	protected static class URIParser {
		private String routingKey = "";
		private String exchangeName = "";
		private String instanceName = null;
		private String scheme = null;
		
		/**
		 * 
		 * Initializes URIParser with a {@link URI}
		 *
		 * @param uri
		 */
		public URIParser(URI uri) {
			this.scheme = uri.getScheme();
			this.routingKey = uri.getPath().substring(1); // skip the leading / 
			this.instanceName = uri.getAuthority();
			Map<String, String> query = getQuery(uri.getQuery());
			if(query != null && query.containsKey(QUERY_KEY_EXCHANGE)) {
				this.exchangeName = query.get(QUERY_KEY_EXCHANGE);
			}
		}
		
		/**
		 * Parses a URI-Query-like string into a {@link Map}
		 * 
		 * @param query the Query-String
		 * @return the parsed {@link Map}
		 */
		private Map<String, String> getQuery(String query) {
			if(query == null) return null;
			Map<String, String> map = new HashMap<String, String>();
			String[] parts = query.split("&");
			for(String part: parts) {
				String[] keyValue = part.split("=");
				if(keyValue.length == 2) {
					map.put(keyValue[0], keyValue[1]);
				}
			}
			return map;
		}
		
		/**
		 * Returns the routingKey.
		 * 
		 * @return the routingKey
		 */
		public String getRoutingKey() {
			return this.routingKey;
		}
		
		/**
		 * Returns the exchangeName.
		 * 
		 * @return the exchangeName
		 */
		public String getExchangeName() {
			return this.exchangeName;
		}

		/**
		 * Returns the instanceName.
		 * 
		 * @return the instanceName
		 */
		public String getInstanceName() {
			return this.instanceName;
		}

		/**
		 * Returns the scheme.
		 * 
		 * @return the scheme
		 */
		public String getScheme() {
			return this.scheme;
		}
	}
}
