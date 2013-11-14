package de.tuberlin.cit.livescale.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.messaging.endpoints.MessageDeliveryEvent;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpointListener;

/**
 * Is the logical Message encapsulation layer. The physical being instances of
 * {@link de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint}
 * 
 * @author Bernd Louis
 */
public final class MessageCenter implements MessageEndpointListener {
	/**
	 * 
	 */
	private static final String CONFIG_KEY_NO_CONCURRENCY = "noConcurrency";
	
	/**
	 * An array of local config keys (for the center itself)
	 */
	private static final String[] LOCAL_CONFIG_KEYS = new String[] {CONFIG_KEY_NO_CONCURRENCY};
	
	/**
	 * default config file filename
	 */
	private static final String CONFIG_FILE_NAME = "livescale-messaging.properties";
	
	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory.getLog(MessageCenter.class);

	/**
	 * Holds the local messageEndpoints
	 */
	private Collection<MessageEndpoint> messageEndpoints;

	/**
	 * Message Listener Map
	 */
	private Map<Class<? extends Message>, Collection<MessageListener<? extends Message>>> messageListeners = 
			new HashMap<Class<? extends Message>, Collection<MessageListener<? extends Message>>>();

	/**
	 * A reference to one {@link ConnectionEventListener}
	 */
	private ConnectionEventListener connectionEventListener;
	
	/**
	 * A list of references to {@link MessageDeliveryEventListener}s to deliver
	 * {@link MessageDeliveryEvent}s
	 */
	private Collection<MessageDeliveryEventListener> messageDeliveryEventListeners = 
			new LinkedList<MessageDeliveryEventListener>();
	/**
	 * holds a map of Request UUIDs and their respective response addresses so
	 * we know the way to sent respective {@link ResponseMessage}s to.
	 */
	private ConcurrentMap<UUID, ResponseContract> requestUUID2ResponseAddresses = new ConcurrentHashMap<UUID, ResponseContract>();
	
	/**
	 * Configuration for the center, whether it should start all endpoints right away
	 */
	private boolean autoStartEndpoints = false;
	
	/**
	 * Configuration for the center indicating whether {@link MessageEndpoint}s
	 * should be started concurrently.
	 */
	private boolean noConcurrency = false;
	
	/**
	 * Helper class describing the response
	 * 
	 * ATTN: This class has no field encapsulation
	 * 
	 * @author louis
	 */
	private class ResponseContract {
		public URI responseURI;
		public long issuedAt;

		public ResponseContract(URI responseURI) {
			this.responseURI = responseURI;
			this.issuedAt = System.currentTimeMillis();
		}

		/**
		 * indicates that a
		 * 
		 * @return
		 */
		public boolean hasTimedOut() {
			return System.currentTimeMillis() > this.issuedAt
					+ this.getTimeout();
		}

		/**
		 * returns the timeout duration
		 * 
		 * @return
		 */
		private long getTimeout() {
			return 1000L * 60L; // 60 seconds
		}
	}
	
	private Thread maintenance = new Thread() {
		@Override
		public void run() {
			while(!Thread.interrupted()) {
				Iterator<Entry<UUID, ResponseContract>> it = 
						MessageCenter.this.requestUUID2ResponseAddresses.entrySet().iterator();
				while(it.hasNext()) {
					Map.Entry<UUID, ResponseContract> entry = it.next();
					if(entry.getValue().hasTimedOut()) {
						it.remove();
					}
				}
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	};
	
	/**
	 * Creates the {@link MessageCenter} by auto-discovering its configuration
	 * according to the local {@link ClassLoader}
	 * 
	 * @throws IOException
	 *             if the properties stream could not be opened
	 * 
	 */
	public MessageCenter(boolean autoStartEndpoints) throws IOException {
		this.setAutoStartEndpoints(autoStartEndpoints);
		URL url = this.getClass().getClassLoader()
				.getResource(MessageCenter.CONFIG_FILE_NAME);
		LOG.debug(String.format("Trying to open infered config %s", url));
		InputStream is = this.getClass().getClassLoader()
				.getResourceAsStream(MessageCenter.CONFIG_FILE_NAME);

		Properties properties = new Properties();
		properties.load(is);
		configureCenter(properties);
		this.configureEndpoints(this.buildEndpointConfigMap(properties));
		this.maintenance.start();
		LOG.info(String
				.format("MessageCenter init with infered config %s", url));
	}


	/**
	 * Creates the {@link MessageCenter} with a specific configuration
	 * 
	 * @param config
	 */
	public MessageCenter(boolean autoStartEndpoints, Properties properties) {
		this.setAutoStartEndpoints(autoStartEndpoints);
		if(properties == null) {
			properties = new Properties();
		}
		configureCenter(properties);
		this.configureEndpoints(this.buildEndpointConfigMap(properties));
		this.maintenance.start();
		LOG.info(String.format("MessageCenter init with provided config"));
	}
	
	/**
	 * 
	 * @param properties
	 */
	private void configureCenter(Properties properties) {
		if(Boolean.valueOf(properties.getProperty(CONFIG_KEY_NO_CONCURRENCY)) == true) {
			this.noConcurrency = true;
		}
	}
	/**
	 * configures the class by instatiating the messageEndpoints
	 * 
	 * This class does "best effort", on problems it will report to the Log.
	 * 
	 */
	private void configureEndpoints(Map<String, Map<String, String>> confmap) {
		this.messageEndpoints = new LinkedList<MessageEndpoint>();
		for (String domain : confmap.keySet()) {
			LOG.debug(String.format("Trying to initialize and configure %s",
					domain));
			this.addEndpoint(domain, confmap.get(domain));
		}
		LOG.debug(String.format(
				"MessageCenter is configured with %d endpoint(s)",
				this.messageEndpoints.size()));
	}

	/**
	 * Creates and initializes a {@link MessageEndpoint} and adds it to the
	 * local list of messageEndpoints.
	 * 
	 * @param domainConf
	 */
	public void addEndpoint(String name, Map<String, String> domainConf) {
		String className = domainConf.get("class");
		MessageEndpoint currentEp = null;
		try {
			currentEp = (MessageEndpoint) Class.forName(className)
					.newInstance();
			currentEp.setName(name);
			currentEp.configure(domainConf);
			addEndpoint(currentEp);
		} catch (InstantiationException e) {
			LOG.warn(String.format("Could not instantiate Endpoint %s",
					className), e);
		} catch (IllegalAccessException e) {
			LOG.warn(String.format("Could not access endpoint %s", className),
					e);
		} catch (ClassNotFoundException e) {
			LOG.warn(String.format("Could not find endpoint %s", className), e);
		}
	}
	
	/**
	 * Directly adds a {@link MessageEndpoint} starting it if
	 * autostart is configured this way. 
	 * 
	 * @param messageEndpoint
	 */
	public void addEndpoint(MessageEndpoint messageEndpoint) {
		messageEndpoint.addMessageEndpointListener(this);
		this.messageEndpoints.add(messageEndpoint);
		if(this.autoStartEndpoints) {
			launchEndpoint(messageEndpoint);
		}
	}
	
	/**
	 * Starts all {@link MessageEndpoint}s
	 */
	public void startAllEndpoints() {
		for(MessageEndpoint me : this.messageEndpoints) {
			launchEndpoint(me);
		}
	}
	
	private void launchEndpoint(final MessageEndpoint me) {
		if(this.noConcurrency) {
			me.start();
		}
		else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					me.start();
				}
			}).start();
		}
	}
	
	/**
	 * Shuts down all {@link MessageEndpoint}s
	 */
	public void shutdownAllEndpoints() {
		if(this.maintenance != null) {
			this.maintenance.interrupt();
		}
		for(MessageEndpoint me : this.messageEndpoints) {
			me.shutdown();
		}
	}

	public void startEndpoint(String name) {
		MessageEndpoint ep = findEndpointOrThrow(name);
		launchEndpoint(ep);
	}

	public void stopEndpoint(String name) {
		MessageEndpoint ep = findEndpointOrThrow(name);
		ep.shutdown();
	}
	
	public boolean allEndpointsRunning() {
		for(MessageEndpoint ep: this.messageEndpoints) {
			if(!(ep.isRunning())) {
				return false;
			}
		}
		return true;
	}
	
	private MessageEndpoint findEndpointOrThrow(String name) {
		MessageEndpoint ep = findEndpointByName(name);
		if(ep == null) {
			throw new IllegalArgumentException(
					String.format("Endpoint %s has not been added with this MessageCenter", name)
			);
		}
		return ep;
	}
	
	private MessageEndpoint findEndpointByName(String name) {
		for(MessageEndpoint me : this.messageEndpoints) {
			if(me.getName().equals(name)) {
				return me;
			}
		}
		return null;
	}
	
	/**
	 * converts the local properties to a map Domain => Property => Value
	 * 
	 * @return
	 */
	private Map<String, Map<String, String>> buildEndpointConfigMap(
			Properties properties) {
		Map<String, Map<String, String>> confmap = new HashMap<String, Map<String, String>>();
		Pattern epClass = Pattern.compile("(\\w+)\\.(\\w+)");
		List<String> centerKeys = Arrays.asList(LOCAL_CONFIG_KEYS);
		for (Object k : properties.keySet()) {
			String entry = (String) k;
			Matcher m = epClass.matcher(entry);
			if (centerKeys.contains(entry) || !m.matches()) {
				LOG.warn(String.format("Ambiguous config entry %s is ignored.",
						entry));
				continue;
			}
			String domain = m.group(1); // the domain
			String property = m.group(2); // property
			String value = properties.getProperty(entry);
			LOG.trace(String.format("%s:%s=%s", domain, property, value));
			if (!confmap.containsKey(domain)) {
				confmap.put(domain, new HashMap<String, String>()); // lazy
																	// instantiation
			}
			confmap.get(domain).put(property, value);
		}
		return confmap;
	}

	/**
	 * Finds a {@link MessageEndpoint} for a given URI
	 * 
	 * @param uri
	 * @return
	 */
	private MessageEndpoint findBestMessageEndpointForURI(URI uri) {
		LOG.trace(String.format("Finding best endpoint for URI: %s", uri));
		boolean needSpecificAuthority = uri.getAuthority() != null && !(uri.getAuthority().isEmpty());
		for (MessageEndpoint e : this.messageEndpoints) { 
			if(		e.getLocalEndpointURI().getScheme().equals(uri.getScheme()) && 
					(	!needSpecificAuthority || 
						e.getLocalEndpointURI().getAuthority().equals(uri.getAuthority())
					)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Returns true if this instance has a message endpoint for a specified URI
	 * 
	 * @param uri
	 * @return
	 */
	public boolean hasEndpointToURI(URI uri) {
		return this.findBestMessageEndpointForURI(uri) != null;
	}
	
	/**
	 * Returns true if it's possible to send a message to a given URI
	 * (is there an endpoint and is it running?)
	 * 
	 * @param uri
	 * @return
	 */
	public boolean canSendToURI(URI uri) {
		MessageEndpoint ep = findBestMessageEndpointForURI(uri);
		return ep != null && ep.isRunning();
	}
	
	/**
	 * 
	 * 
	 * @param messages
	 * @param targetURI
	 */
	public void send(Collection<Message> messages, URI targetURI) {
		MessageManifest manifest = new MessageManifest(messages, targetURI);
		this.sendManifest(manifest);
	}

	/**
	 * 
	 * @param targetURI
	 * @param message
	 */
	public void send(Message message, URI targetURI) {
		this.sendManifest(new MessageManifest(message, targetURI));
	}

	public void send(RequestMessage requestMessage, URI targetURI,
			URI responseURI) {
		MessageManifest manifest = new MessageManifest(requestMessage,
				targetURI, responseURI);
		this.sendManifest(manifest);
	}

	private void sendManifest(MessageManifest manifest) {
		MessageEndpoint ep = this.findBestMessageEndpointForURI(manifest
				.getTargetURI());
		if (ep == null) {
			throw new IllegalArgumentException(String.format(
					"Could not find Endpoint for targetURI %s", manifest
							.getTargetURI().toString()));
		}
		ep.send(manifest);
	}

	/**
	 * sends a response to a request-message, the MessageCenter will find out
	 * the {@link URI} automatically.
	 * 
	 * @param responseMessage
	 */
	public void sendResponse(ResponseMessage responseMessage) {
		URI responseURI = this.requestUUID2ResponseAddresses
				.remove(responseMessage.getRequestMessageUUID()).responseURI;
		this.send(responseMessage, responseURI);
	}

	/**
	 * Adds a listener instance for a specific Message Class.
	 * 
	 * @param messageClass
	 *            the {@link Message} instance you'd like to subscribe to.
	 * @param messageListener
	 *            a listener instance
	 */
	public void addMessageListener(Class<? extends Message> messageClass,
			MessageListener<? extends Message> messageListener) {
		if (!this.messageListeners.containsKey(messageClass)) { // laaaaaazy
			this.messageListeners.put(messageClass,
					new LinkedList<MessageListener<? extends Message>>());
		}
		this.messageListeners.get(messageClass).add(messageListener);
	}
	
	
	public void addMessageDeliveryEventListener(MessageDeliveryEventListener listener) {
		this.messageDeliveryEventListeners.add(listener);
	}
	
	private void notifyMessageDeliveryEventListeners(MessageDeliveryEvent event) {
		for(MessageDeliveryEventListener l : this.messageDeliveryEventListeners) {
			l.handleMessageEvent(event);
		}
	}
	
	/**
	 * TODO Remove SuppressWarnings for reasons unknown <code>? extends Message</code>
	 * does not work.  
	 * 
	 * @param message
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void notifyMessageListeners(Message message) {
		final Collection<MessageListener<? extends Message>> listeners = this.messageListeners
				.get(message.getClass());
		if (listeners == null) {
			return; 
		}
		for (MessageListener ml : listeners) {
			ml.handleMessageReceived(message);
		}
	}

	/**
	 * Set the listener to Connection Events here. "There can be only one"(TM)
	 * 
	 * @param connectionEventListener
	 */
	public void setConnectionEventListener(
			ConnectionEventListener connectionEventListener) {
		this.connectionEventListener = connectionEventListener;
	}

	// methods from IFACE MessageEndpointListener

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpointListener
	 * #handleMessageReceived(de.tuberlin.cit.livescale.messaging.Message)
	 */
	@Override
	public void handleMessageReceived(MessageManifest messageManifest) {
		for (Message m : messageManifest.getMessages()) {
			if (m instanceof RequestMessage) {
				UUID requestMessageUUID = ((RequestMessage) m).getUUID();
				this.requestUUID2ResponseAddresses.put(requestMessageUUID,
						new ResponseContract(messageManifest.getResponseURI()));
			}
			this.notifyMessageListeners(m);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpointListener
	 * #handleDeliveryError()
	 */
	@Override
	public void handleDeliveryEvent(MessageDeliveryEvent messageDeliveryEvent) {
		notifyMessageDeliveryEventListeners(messageDeliveryEvent);
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpointListener#handleShutdown(de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint)
	 */
	@Override
	public void handleShutdown(MessageEndpoint endpoint) {
		this.messageEndpoints.remove(endpoint);
	}

	/**
	 * Returns the autoStartEndpoints.
	 * 
	 * @return the autoStartEndpoints
	 */
	public boolean isAutoStartEndpoints() {
		return this.autoStartEndpoints;
	}

	/**
	 * Sets the autoStartEndpoints to the specified value.
	 *
	 * @param autoStartEndpoints the autoStartEndpoints to set
	 */
	public void setAutoStartEndpoints(boolean autoStartEndpoints) {
		this.autoStartEndpoints = autoStartEndpoints;
	}
}