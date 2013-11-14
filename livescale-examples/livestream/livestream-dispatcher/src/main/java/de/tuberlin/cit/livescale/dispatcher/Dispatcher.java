package de.tuberlin.cit.livescale.dispatcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.dispatcher.db.DispatcherDb;
import de.tuberlin.cit.livescale.dispatcher.db.DispatcherDbInterface;
import de.tuberlin.cit.livescale.dispatcher.db.model.Follower;
import de.tuberlin.cit.livescale.dispatcher.db.model.Stream;
import de.tuberlin.cit.livescale.dispatcher.db.model.User;
import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.MessageDeliveryEventListener;
import de.tuberlin.cit.livescale.messaging.MessageListener;
import de.tuberlin.cit.livescale.messaging.RequestMessage;
import de.tuberlin.cit.livescale.messaging.ResponseMessage;
import de.tuberlin.cit.livescale.messaging.endpoints.AMQPEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageDeliveryEvent;
import de.tuberlin.cit.livescale.messaging.endpoints.TargetMovedFailure;
import de.tuberlin.cit.livescale.messaging.endpoints.TargetMovedSuccess;
import de.tuberlin.cit.livescale.messaging.messages.ClientFavoriteStreamStarted;
import de.tuberlin.cit.livescale.messaging.messages.ClientFollowerAnswer;
import de.tuberlin.cit.livescale.messaging.messages.ClientRegistrationAnswer;
import de.tuberlin.cit.livescale.messaging.messages.ClientStreamRcv;
import de.tuberlin.cit.livescale.messaging.messages.ClientStreamSend;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherGCM;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRegistration;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestFollower;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestStreamRcv;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestStreamSend;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamClose;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamConfirm;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamStatus;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverNewStream;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverRequestStreamStatus;

/**
 * Dispatcher Server
 * 
 * @author Bernd Louis (bernd.louis@gmail.com)
 */
public class Dispatcher implements MessageDeliveryEventListener {
	
	private final static String ROUTING_KEY_STREAMSERVER = "streamserver";
	private static final String CIT_STREAM_EXCHANGE = "cit_stream_exchange";
	/**
	 * local logger
	 */
	private static final Log LOG = LogFactory.getLog(Dispatcher.class);
	/**
	 * Instance of {@link MessageCenter} for incoming and outgoing
	 * {@link Message}s
	 */
	private MessageCenter messageCenter;
	/**
	 * Local (derby) database to hold the state
	 */
	private DispatcherDbInterface db = DispatcherDb.getInterface();
	
	/**
	 * Stores pending {@link Message}s that are waiting for another
	 * event to happen (for instance the response from another peer
	 * like the Stream-Server)
	 */
	private PendingMessagesCache pendingMessages = new PendingMessagesCache();
	
	
	/**
	 * 
	 * Initializes Dispatcher.
	 * 
	 * @throws IOException
	 */
	public Dispatcher() throws IOException {
		this.messageCenter = new MessageCenter(false); 
		setupMessageReceiveHandlers();
		this.messageCenter.addMessageDeliveryEventListener(this);
		this.messageCenter.startAllEndpoints();
		try {
			new URI("amqp:///" + ROUTING_KEY_STREAMSERVER);
		} catch (URISyntaxException e) { // should never happen.
			LOG.fatal("Cannot build Stream-Server URI", e);
		}
		// before the JVM halts
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    @Override
			public void run() { 
		    	Dispatcher.this.shutdown();
		    }
		 });
		LOG.info("Dispatcher started");
	}

	/**
	 * Registers the message listeners to the {@link MessageCenter}
	 */
	private void setupMessageReceiveHandlers() {
		this.messageCenter.addMessageListener(DispatcherGCM.class, this.dispatcherC2DMListener);
		this.messageCenter.addMessageListener(DispatcherRegistration.class, this.dispatcherRegistrationListener);
		this.messageCenter.addMessageListener(DispatcherRequestFollower.class, this.dispatcherRequestFollowerListener);
		this.messageCenter.addMessageListener(DispatcherRequestStreamRcv.class, this.dispatcherRequestStreamRcvListener);
		this.messageCenter.addMessageListener(DispatcherRequestStreamSend.class, this.dispatcherRequestStreamSendListener);
		this.messageCenter.addMessageListener(DispatcherStreamClose.class, this.dispatcherStreamCloseListener);
		this.messageCenter.addMessageListener(DispatcherStreamConfirm.class, this.dispatcherStreamConfirmListener);
		this.messageCenter.addMessageListener(DispatcherStreamStatus.class, this.dispatcherStreamStatusListener);
	}
	
	private MessageListener<DispatcherStreamStatus> dispatcherStreamStatusListener = 
			new MessageListener<DispatcherStreamStatus>() {
				@Override
				public void handleMessageReceived(DispatcherStreamStatus message) {
					LOG.debug("Received DispatcherStreamStatus");
					if(	message.getSendEndpointToken().isEmpty() ||
						message.getReceiveEndpointToken().isEmpty() ||
						message.isActive()) {
						return;
					}
					LOG.debug("Updating Stream token (deleting it)");
					Stream stream = Dispatcher.this.db.findStreamByRcvToken(message.getReceiveEndpointToken());
					if(stream != null) {
						Dispatcher.this.db.removeObject(stream);
					}
				}
			};
	
	private MessageListener<DispatcherStreamConfirm> dispatcherStreamConfirmListener = 
			new MessageListener<DispatcherStreamConfirm>() {
				@Override
				public void handleMessageReceived(DispatcherStreamConfirm message) {
					LOG.debug("Received DispatcherStreamConfirm");
					String username = message.getUsername();
					if(message.getReceiveEndpointAddress().isEmpty() || 
						message.getReceiveEndpointPort() == 0 ||
						message.getReceiveEndpointToken().isEmpty() ||
						message.getSendEndpointAddress().isEmpty() ||
						message.getSendEndpointPort() == 0 ||
						message.getSendEndpointToken().isEmpty() || 
						username.isEmpty()) {
						return;
					}
					User user = Dispatcher.this.db.findUserByUsername(username);
					if(user == null) {
						return;
					}
					Stream stream = Dispatcher.this.db.findStreamByRcvToken(message.getReceiveEndpointToken());
					if(stream == null) {
						stream = new Stream();
					}
					stream.setRcvAddress(message.getReceiveEndpointAddress());
					stream.setRcvPort(message.getReceiveEndpointPort());
					stream.setRcvToken(message.getReceiveEndpointToken());
					stream.setSendAddress(message.getSendEndpointAddress());
					stream.setSendPort(message.getSendEndpointPort());
					stream.setSendToken(message.getSendEndpointToken());
					stream.setUser(user);
					try {
						Dispatcher.this.db.updateObject(stream);
						DispatcherRequestStreamSend drss = (DispatcherRequestStreamSend) Dispatcher.this.pendingMessages.remove(message);
						if(drss == null) {
							LOG.warn("Could not forward message because message author is orphaned.");
							return;
						}
						// initialize with message
						ClientStreamSend css = new ClientStreamSend(drss.getUUID());
						css.setUsername(message.getUsername());
						css.setReceiveEndpointAddress(message.getReceiveEndpointAddress());
						css.setReceiveEndpointPort(message.getReceiveEndpointPort());
						css.setReceiveEndpointToken(message.getReceiveEndpointToken());
						css.setSendEndpointAddress(message.getSendEndpointAddress());
						css.setSendEndpointPort(message.getSendEndpointPort());
						css.setSendEndpointToken(message.getSendEndpointToken());
						Dispatcher.this.messageCenter.sendResponse(css);
						// TODO Multicasts
						List<Follower> followers = Dispatcher.this.db.findFollowersByUser(user);
						LOG.debug(String.format("Sending GCM notifications to %d users", followers.size()));
						for(Follower follower : followers) {
							User followingUser = follower.getFollower();
							try {
								URI targetURI = new URI("gcm:///" + followingUser.getC2dmKey());
								ClientFavoriteStreamStarted cfss = new ClientFavoriteStreamStarted();
								cfss.setUsername(user.getUsername());
								cfss.setReceiveToken(message.getReceiveEndpointToken());
								Dispatcher.this.messageCenter.send(cfss, targetURI);
							}
							catch(URISyntaxException e) {
								LOG.warn("Could not encode URI while sending GCM Message", e);
							}
						}
					}
					catch(Exception e) {
						LOG.debug("Stream could not be persisted.", e);
					}
				}
			};
	
	private MessageListener<DispatcherRequestStreamSend> dispatcherRequestStreamSendListener =
			new MessageListener<DispatcherRequestStreamSend>() {
				
				@Override
				public void handleMessageReceived(DispatcherRequestStreamSend message) {
					// malformed message? 
					LOG.debug("Received DispatcherRequestStreamSend");
					String username = message.getUsername();
					String password = message.getPassword();
					if(username.isEmpty() || password.isEmpty()) {
						return;
					}
					User user = Dispatcher.this.db.findUserByUsername(username);
					if(user == null || !user.getPassword().equals(password)) {
						return;
					}
					Stream stream = Dispatcher.this.db.findStreamByUser(user);
					RequestMessage streamServerRequest = null;
					URI uri = null;
					if(stream != null) {
						LOG.debug("Dispatching to Server StreamserverRequestStreamStatus Request");
						StreamserverRequestStreamStatus status = new StreamserverRequestStreamStatus();
						status.setReceiveEndpointToken(stream.getRcvToken());
						status.setSendEndpointToken(stream.getSendToken());
						streamServerRequest = status;
						try {
							uri = new AMQPEndpoint.URIBuilder().
									routingKey(new String[] {"broadcast", ROUTING_KEY_STREAMSERVER}).
									exchangeName(CIT_STREAM_EXCHANGE).
									build();
						} catch (URISyntaxException e) {
							LOG.error("Could not encode Streamserver target URI, this should never happen", e);
						}
					}
					else {
						LOG.debug("Dispatching to Server with StreamserverNewStream request");
						StreamserverNewStream newstream = new StreamserverNewStream();
						newstream.setUsername(username);
						newstream.setSendEndpointToken(UUID.randomUUID().toString());
						newstream.setReceiveEndpointToken(UUID.randomUUID().toString());
						streamServerRequest = newstream;
						try {
							uri = new AMQPEndpoint.URIBuilder().
									routingKey(new String[]{"task", Dispatcher.ROUTING_KEY_STREAMSERVER}).
									exchangeName(CIT_STREAM_EXCHANGE).
									build();
						} catch (URISyntaxException e) {
							LOG.error("Could not encode Streamserver target URI, this should never happen", e);
						}
					}
					Dispatcher.this.messageCenter.send(streamServerRequest, uri);
					Dispatcher.this.pendingMessages.add(message, streamServerRequest);
				}
			};
	
	private MessageListener<DispatcherStreamClose> dispatcherStreamCloseListener = 
			new MessageListener<DispatcherStreamClose>() {
				
				@Override
				public void handleMessageReceived(DispatcherStreamClose message) {
					LOG.debug("Received DispatcherStreamClose");
					if(message.getSendToken().isEmpty() || message.getReceiveToken().isEmpty()) {
						return;
					}
					
					Stream stream = Dispatcher.this.db.findStreamByRcvToken(message.getReceiveToken());
					if(stream != null) {
						Dispatcher.this.db.removeObject(stream);
					}
				}
			};
	
	
	private MessageListener<DispatcherRequestStreamRcv> dispatcherRequestStreamRcvListener = 
			new MessageListener<DispatcherRequestStreamRcv>() {
				@Override
				public void handleMessageReceived(DispatcherRequestStreamRcv message) {
					LOG.debug("Received DispatcherRequestStreamRcv");
					String token = message.getToken();
					if(token.isEmpty()) { // malformed message
						return;
					}
					Stream stream = Dispatcher.this.db.findStreamByRcvToken(token);
					if(stream == null) { // Stream not found.
						return;
					}
					LOG.debug("Sending ClientStreamRcv Response");
					ClientStreamRcv response = (ClientStreamRcv) message.getResponseMessage();
					response.setAddress(stream.getRcvAddress());
					response.setPort(stream.getRcvPort());
					response.setToken(stream.getRcvToken());
					response.setUserNameSender(stream.getUser().getUsername());
					Dispatcher.this.messageCenter.sendResponse(response);
				}
			};
			
	private MessageListener<DispatcherRequestFollower> dispatcherRequestFollowerListener = 
			new MessageListener<DispatcherRequestFollower>() {
				
				@Override
				public void handleMessageReceived(DispatcherRequestFollower message) {
					// message malformed? 
					LOG.debug("Received DispatcherRequestFollower");
					String username = message.getUsername();
					String password = message.getPassword();
					String followerUsername = message.getFollowerUsername();
					if(username.isEmpty() || password.isEmpty()) {
						return;
					}
					User user = Dispatcher.this.db.findUserByUsername(username);
					User favorite = Dispatcher.this.db.findUserByUsername(followerUsername);
					if(	user == null || // user not found 
						!user.getPassword().equals(password) || // password does not match 
						favorite == null || // favorite not found
						user.equals(followerUsername)) { // user is the favorite
						return;
					}
					Follower follower = Dispatcher.this.db.findFollowerByUserAndFavorite(user, favorite);
					int resultType = -1;
					int followRequest = message.getFollowRequest();
					if(followRequest == -1 && follower != null) {
						LOG.debug("Removing follower state");
						Dispatcher.this.db.removeObject(follower);
					}
					else if (followRequest == 0 && follower != null) {
						LOG.debug("Returning followerState true");
						resultType = 1;
					}
					else if(followRequest == 1) {
						if(follower == null) {
							LOG.debug("Setting FollowerState");
							follower = new Follower();
							follower.setUser(favorite);
							follower.setFollower(user);
							Dispatcher.this.db.persistObject(follower);
						}
						resultType = 1;
					}
					else { // invalid followRequest 
						return;
					}
					ClientFollowerAnswer response = (ClientFollowerAnswer) message.getResponseMessage();
					response.setFollowerResult(resultType);
					Dispatcher.this.messageCenter.sendResponse(response);

				}
			};
	
	private MessageListener<DispatcherGCM> dispatcherC2DMListener =
			new MessageListener<DispatcherGCM>() {
				
				@Override
				public void handleMessageReceived(DispatcherGCM message) {
					LOG.debug("Received DispatcherGCM");
					if (message.getUsername().isEmpty()
							|| message.getPassword().isEmpty()) {
						return;
					}
					User user = Dispatcher.this.db.findUserByUsername(message
							.getUsername());
					if (user == null
							|| !user.getPassword().equals(message.getPassword())) {
						return;
					}
					user.setC2dmKey(message.getC2dmKey());
					Dispatcher.this.db.updateObject(user);
					Dispatcher.LOG.debug(String.format(
							"Updated the C2DM token for user %s", user.getUsername()));
				}
			};
	
	private MessageListener<DispatcherRegistration> dispatcherRegistrationListener = 
			new MessageListener<DispatcherRegistration>() {
				
				@Override
				public void handleMessageReceived(DispatcherRegistration message) {
					LOG.debug("Received DispatcherRegistration");
					ClientRegistrationAnswer response = (ClientRegistrationAnswer) message
							.getResponseMessage();
					response.setUsername("");
					String username = message.getUsername();
					User existingUser = Dispatcher.this.db.findUserByUsername(username);
					String password = message.getPassword();
					
					// malformed message, username or password is empty
					if (username.isEmpty() || password.isEmpty()) {
						LOG.debug("Malformed DispatcherRegistration message");
						response.setErrorMessage("Username or password invalid!");
						response.setOk(false);
					} else if (existingUser != null) { // user exists and password matches
						if (password.equals(existingUser.getPassword())) {
							LOG.debug("Account has been verified.");
							response.setErrorMessage("Account verified.");
							response.setOk(true);
						}
						else { // user exists but password does not patch
							LOG.debug("Username / Password invalid");
							response.setErrorMessage("Username or password invalid");
							response.setOk(false);
						}
					}
					else { // register the user
						try {
							LOG.debug("User has been registered");
							User user = new User();
							user.setUsername(username);
							user.setPassword(password);
							Dispatcher.this.db.persistObject(user);
							response.setErrorMessage("User created");
							response.setOk(true);
						}
						catch(Exception e) { 
							LOG.error("Error while persisting the user to the database", e);
							response.setErrorMessage("Database error");
							response.setOk(false);
						}
					}
					Dispatcher.this.messageCenter.sendResponse(response);
				}
			};
	
	/**
	 * This helper encapsulates a map that 
	 * 
	 * @author Bernd Louis
	 */
	private class PendingMessagesCache {
		/**
		 * Maps from the forwarded Messages UUID to the original Message
		 */
		private ConcurrentMap<UUID, MemoInfo> memos = new ConcurrentHashMap<UUID, Dispatcher.PendingMessagesCache.MemoInfo>(); 
		private Thread maintenance;
		
		private long timeout = TimeUnit.SECONDS.toMillis(60);
		
		public PendingMessagesCache() {
			this.maintenance = new Thread(new Runnable() {
				@Override
				public void run() {
					while(!(Thread.interrupted())) {
						// remove all 
						long now = System.currentTimeMillis();
						Iterator<Entry<UUID, MemoInfo>> it = PendingMessagesCache.this.memos.entrySet().iterator(); 
						while(it.hasNext()) {
							Map.Entry<UUID, MemoInfo> entry = it.next();
							if(entry.getValue().timestamp + PendingMessagesCache.this.timeout < now) {
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
			});
			this.maintenance.start();
		}
		
		public void add(RequestMessage pendingMessage, RequestMessage forwardMessage) {
			this.memos.put(forwardMessage.getUUID(), new MemoInfo(pendingMessage));
		}
		
		public RequestMessage remove(ResponseMessage chainedAnswerMessage) {
			 MemoInfo info = this.memos.remove(chainedAnswerMessage.getRequestMessageUUID());
			 if(info != null) {
				 return info.pendingMessage; 
			 }
			 return null;
		}
		
		private class MemoInfo {
			private long timestamp;
			private RequestMessage pendingMessage;
			
			public MemoInfo(RequestMessage pendingMessage) {
				this.timestamp = System.currentTimeMillis();
				this.pendingMessage = pendingMessage;
			}
		}
		
		/**
		 * Shuts down the cache, please use this method to
		 * kill the maintenance thread
		 */
		public void shutdown() {
			this.maintenance.interrupt();
		}
	}
	
	public void shutdown() {
		LOG.info("Shutting down");
		this.pendingMessages.shutdown();
		this.messageCenter.shutdownAllEndpoints();
	}
	
	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.MessageDeliveryEventListener#handleMessageEvent(de.tuberlin.cit.livescale.messaging.endpoints.MessageDeliveryEvent)
	 */
	@Override
	public void handleMessageEvent(MessageDeliveryEvent messageDeliveryEvent) {
		// target has a new gcmKey
		if(messageDeliveryEvent instanceof TargetMovedSuccess) {
			TargetMovedSuccess tms = (TargetMovedSuccess) messageDeliveryEvent;
			// we only handle gcm events here
			URI targetURI = tms.getMessageManifest().getTargetURI();
			if(!(targetURI.getScheme().equals("gcm"))) {
				return;
			}
			String gcmKey = targetURI.getPath().substring(1);
			User user = this.db.findUserbyC2DMḰey(gcmKey);
			if(user == null) { // couldn't find user
				LOG.warn(String.format("Could not resolve user with gcmKey (currently field c2dmKey): %s for update", gcmKey));
				return;
			}
			user.setC2dmKey(tms.getNewTargetURI().getPath().substring(1));
			this.db.updateObject(user);
		}
		// target not found in the gcm network, we're resetting the key to nothing
		else if(messageDeliveryEvent instanceof TargetMovedFailure) {
			TargetMovedFailure tmf = (TargetMovedFailure) messageDeliveryEvent;
			URI targetURI = tmf.getMessageManifest().getTargetURI();
			if(!(targetURI.getScheme().equals("gcm"))) {
				return;
			}
			String gcmKey = targetURI.getPath().substring(1);
			User user = this.db.findUserbyC2DMḰey(gcmKey);
			if(user == null) {
				LOG.warn(String.format("Could not resolve user with gcmKey %s for invalidation", gcmKey));
				return;
			}
			user.setC2dmKey(null);
			this.db.updateObject(user);
		}
	}
	
	/**
	 * Starts the {@link Dispatcher} program.
	 * 
	 * @param args
	 *            Takes no arguments at the moment
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// new DispatcherCommunicationManager("localhost", null, null);
		try {
			new Dispatcher();
		} catch (IOException e) {
			LOG.fatal("Dispatcher could not be started.", e);
		}
	}

}
