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
package de.tuberlin.cit.livestream.android.messaging;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import de.tuberlin.cit.livestream.android.Constants;
import de.tuberlin.cit.livestream.android.Helper;
import de.tuberlin.cit.livestream.android.activity.Main;
import de.tuberlin.cit.livestream.android.activity.Videoplayer;
import de.tuberlin.cit.livestream.android.broadcast.BCConstants;
import de.tuberlin.cit.livestream.android.view.SetupStreamingAsyncTask;
import de.tuberlin.cit.livestream.messaging.MessageCenter;
import de.tuberlin.cit.livestream.messaging.MessageDeliveryEventListener;
import de.tuberlin.cit.livestream.messaging.MessageListener;
import de.tuberlin.cit.livestream.messaging.endpoints.AMQPEndpoint;
import de.tuberlin.cit.livestream.messaging.endpoints.MessageDeliveryEvent;
import de.tuberlin.cit.livestream.messaging.messages.ClientFollowerAnswer;
import de.tuberlin.cit.livestream.messaging.messages.ClientRegistrationAnswer;
import de.tuberlin.cit.livestream.messaging.messages.ClientStreamRcv;
import de.tuberlin.cit.livestream.messaging.messages.ClientStreamSend;
import de.tuberlin.cit.livestream.messaging.messages.DispatcherGCM;
import de.tuberlin.cit.livestream.messaging.messages.DispatcherRegistration;
import de.tuberlin.cit.livestream.messaging.messages.DispatcherRequestFollower;
import de.tuberlin.cit.livestream.messaging.messages.DispatcherRequestStreamRcv;
import de.tuberlin.cit.livestream.messaging.messages.DispatcherRequestStreamSend;

/**
 * This singleton encapsulates 
 * 
 * @author Bernd Louis
 *
 */
public class Messaging {
	private static final String CIT_STREAM_EXCHANGE = "cit_stream_exchange";
	private static final String ROUTING_KEY_DISPATCHER = "dispatcher";
	private final static String ROUTING_KEY_BROADCAST = "broadcast";
	private final static String ROUTING_KEY_TASK = "task";
	
	/**
	 * Tag for Android's {@link Log}ger
	 */
	private static String TAG = Messaging.class.getSimpleName();
	
	/**
	 * Singleton instance
	 */
	private static Messaging messaging = new Messaging();
	
	private Context context = null;
	private SharedPreferences preferences = null;
	private MessageCenter messageCenter = null;
	
	/**
	 * Client contexts ... 
	 * concurrency will save us some synchronization efforts
	 */
	private final Set<String> clients = 
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	private final SharedPreferences.OnSharedPreferenceChangeListener prefsChangeListener = 
			new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
						final String key) {
					// got new connectivity coordinates
					if(key.equals("server_address") || key.equals("server_port")) {
						if(Messaging.this.messageCenter != null) {
							invalidateCenter();
						}
						buildCenter();
					}
				}
			}; 
	
	private final MessageDeliveryEventListener messageDeliveryEventListener = 
			new MessageDeliveryEventListener() {
				@Override
				public void handleMessageEvent(final MessageDeliveryEvent arg0) {
					// TODO 
				}
			};
	/**
	 * Singleton constructor
	 * 
	 * Initializes Messaging.
	 *
	 */
	private Messaging() { }
	
	public static Messaging getInstance() {
		return messaging;
	}
	
	/**
	 * Returns true if the {@link MessageCenter}'s ready to
	 * deliver messages. 
	 * 
	 * @return
	 */
	public boolean isReady() {
		return this.messageCenter.allEndpointsRunning();
	}
	
	/**
	 * Adds a {@link Context} to the clients list and then
	 * creates a {@link MessageCenter} if necessary. 
	 * 
	 * @param c
	 */
	public synchronized void attach(final Context c) {
		Log.d(TAG, String.format("Attaching Context %s", c.getClass().getSimpleName()));
		this.context = c.getApplicationContext();
		if(this.preferences == null) {
			this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
			this.preferences.registerOnSharedPreferenceChangeListener(this.prefsChangeListener);
		}
		this.clients.add(c.getClass().getName());
		buildCenter();
	}
	
	/**
	 * Removes an object from the clients list
	 * 
	 * @param obj
	 */
	public synchronized void detach(final Object obj) { // synchronized?!? 
		Log.d(TAG, String.format("Detaching Context %s", obj.getClass().getSimpleName()));
		this.clients.remove(obj.getClass().getName());
		if(this.clients.isEmpty()) { // invalidate
			invalidateCenter();
		}
	}
	
	/**
	 * Safely removes the local {@link MessageCenter}. 
	 * 
	 */
	private void invalidateCenter() {
		synchronized(this.messageCenter) {
			if(this.messageCenter.allEndpointsRunning()) {
				this.messageCenter.shutdownAllEndpoints();
				this.messageCenter = null;
			}
		}
	}
	
	/**
	 * Sets up the local {@link MessageCenter}. 
	 */
	private void buildCenter() {
		// only build if we need one.
		if(this.messageCenter != null || this.clients.isEmpty()) {
			return;
		}
		Log.d(TAG, "Building Center as it's necessary");
		final String brokerAddress = this.preferences.getString(Constants.PREF_SERVER_ADDRESS, "localhost");
		final String brokerPort = this.preferences.getString(Constants.PREF_SERVER_PORT, "5672");
		// p.putAll(this.preferences.getAll());
		
		final Properties p = new AMQPEndpoint.ConfigBuilder().
				brokerAddress(brokerAddress).
				brokerPort(Integer.parseInt(brokerPort)).
				exchangeName("cit_stream_exchange"). 
				routingKey("client.#").
				listenForBroadcasts(false).
				listenForTasks(false).
				build("rabbit");
		
		// create center
		this.messageCenter = new MessageCenter(false, p);
		this.messageCenter.addMessageDeliveryEventListener(this.messageDeliveryEventListener);
		// listeners 
		this.messageCenter.addMessageListener(ClientFollowerAnswer.class, this.clientFollowerAnswerListener);
		this.messageCenter.addMessageListener(ClientRegistrationAnswer.class, this.clientRegistrationAnswerListener);
		this.messageCenter.addMessageListener(ClientStreamRcv.class, this.clientStreamRcvListener);
		this.messageCenter.addMessageListener(ClientStreamSend.class, this.clientStreamSendListener);
		
		// go 
		this.messageCenter.startAllEndpoints();
	}
	
	public void sendDispatcherC2dm(final String username, final String password, final String c2dmKey) {
		Log.d(TAG, "Sending DispatcherC2dm");
		final DispatcherGCM msg = new DispatcherGCM();
		msg.setUsername(username);
		msg.setPassword(password); 
		msg.setC2dmKey(c2dmKey);
		this.messageCenter.send(msg, getDispatcherBroadcastURI());				
	}
	
	public void sendDispatcherRegistration(final String username, final String password) {
		Log.d(TAG, "Sending DispatcherRegistration");
		final DispatcherRegistration msg = new DispatcherRegistration();
		msg.setUsername(username);
		msg.setPassword(password);
		this.messageCenter.send(msg, getDispatcherTaskURI());
	}

	
	public void sendDispatcherRequestFollower(final String username, final String password, final String favorite, final int request) {
		Log.d(TAG, "Sending DispatcherRequestFollower");
		final DispatcherRequestFollower msg = new DispatcherRequestFollower();
		msg.setUsername(username);
		msg.setPassword(password);
		msg.setFollowerUsername(favorite);
		msg.setFollowRequest(request);
		this.messageCenter.send(msg, getDispatcherBroadcastURI());
	}
	
	public void sendDispatcherRequestStreamRcv(final String rcvToken) {
		Log.d(TAG, "Sending DispatcherRequestStreamRcv");
		final DispatcherRequestStreamRcv msg = new DispatcherRequestStreamRcv();
		msg.setToken(rcvToken);
		this.messageCenter.send(msg, getDispatcherBroadcastURI());
	}
	
	public void sendDispatcherRequestStreamSend(final String username, final String password) {
		Log.d(TAG, "Sending DispatcherRequestStreamSend");
		final DispatcherRequestStreamSend msg = new DispatcherRequestStreamSend();
		msg.setUsername(username);
		msg.setPassword(password);
		this.messageCenter.send(msg, getDispatcherBroadcastURI());
	}
	
	/**
	 * creates a {@link URI} suitable for a dispatcher task
	 * 
	 * @return
	 */
	private URI getDispatcherTaskURI() {
		try {
			return new AMQPEndpoint.URIBuilder().
					routingKey(new String[] {ROUTING_KEY_TASK, ROUTING_KEY_DISPATCHER}).
					exchangeName(CIT_STREAM_EXCHANGE).
					build();
		} catch (final URISyntaxException e) {
			Log.wtf(TAG, "Could not encode Task-URI", e);
		}
		return null;
	}
	
	/**
	 * creates a {@link URI} suitable for a dispatcher broadcast.
	 * 
	 * @return
	 */
	private URI getDispatcherBroadcastURI() {
		try {
			return new AMQPEndpoint.URIBuilder().
					routingKey(new String[] {ROUTING_KEY_BROADCAST, ROUTING_KEY_DISPATCHER}).
					exchangeName(CIT_STREAM_EXCHANGE).
					build();
		} catch (final URISyntaxException e) {
			Log.wtf(TAG, "Could not encode URI", e);
		}
		return null;
	}
	
	/**
	 * Listener for {@link ClientFollowerAnswer} {@link Message}s
	 */
	private final MessageListener<ClientFollowerAnswer> clientFollowerAnswerListener = 
			new MessageListener<ClientFollowerAnswer>() {
				@Override
				public void handleMessageReceived(final ClientFollowerAnswer message) {
					Log.d(TAG, "Received ClientFollowerAnswer");
					final int result = message.getFollowerResult();
					final String username = message.getUsername();
					final String followerName = message.getFollowerName();
					if(	username.isEmpty() || 
						followerName.isEmpty() ||
						result == 0) {
						Log.d(TAG, String.format("%s received but was malformed. Discarded.", message.getClass().getSimpleName()));
						return;
					}
					Messaging.this.preferences.edit().
						putInt(Constants.PREF_CUR_STREAM_FAVORITE, result).
						putLong(Constants.PREF_UPDATE_TIME, System.currentTimeMillis()).
						commit();
				}
			};
	
	/**
	 * Listener for {@link ClientRegistrationAnswer} {@link Message}s
	 */
	private final MessageListener<ClientRegistrationAnswer> clientRegistrationAnswerListener = 
			new MessageListener<ClientRegistrationAnswer>() {
				
				@Override
				public void handleMessageReceived(final ClientRegistrationAnswer message) {
					Log.d(TAG, "Received ClientRegistrationAnswer");
					Messaging.this.preferences.edit().
						putBoolean(Constants.PREF_REGISTRATION_OK, message.isOk()).
						putString(Constants.PREF_REGISTRATION_MSG, message.getErrorMessage()).
						putLong(Constants.PREF_UPDATE_TIME, System.currentTimeMillis());
				}
			};
	
	/**
	 * Dismisses ProgressDialog if there is one displaying this is necessary
	 * because of the asynchronous way the communication system works
	 */
	public void dismissProgressDialog() {
		if (Main.mProgressDialog != null)
			Main.mProgressDialog.dismiss();
	}
			
	/**
	 * Listener for {@link ClientStreamRcv} {@link Message}s
	 */
	private final MessageListener<ClientStreamRcv> clientStreamRcvListener = 
			new MessageListener<ClientStreamRcv>() { 
				
				@Override
				public void handleMessageReceived(final ClientStreamRcv message) {
					Log.d(TAG, "Received ClientStreamRcv");
					final Intent videoplayerIntent = new Intent(Messaging.this.context, Videoplayer.class);
					videoplayerIntent.putExtra("MAP_KEY_URL", 
						Helper.encodeHTTPDirectStreamURL(
								message.getAddress(), message.getPort(), message.getToken()
						)
					);
					// user_name_sender is actually 
					// CommunicationManager.MAP_KEY_USER_NAME_SENDER
					videoplayerIntent.putExtra("user_name_sender", message.getUserNameSender());
					videoplayerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					dismissProgressDialog();
					Messaging.this.context.startActivity(videoplayerIntent);
				}
			};
	
	/**
	 * Listener for {@link ClientStreamSend} {@link Message}s
	 */
	private final MessageListener<ClientStreamSend> clientStreamSendListener = 
			new MessageListener<ClientStreamSend>() {
				
				@Override
				public void handleMessageReceived(final ClientStreamSend message) {
					Log.d(TAG, "Received ClientStreamSend");
					final String receiveEndpointAddress = message.getReceiveEndpointAddress();
					final int receiveEndpointPort = message.getReceiveEndpointPort();
					final String receiveEndpointToken = message.getReceiveEndpointToken();
					final String sendEndpointAddress = message.getSendEndpointAddress();
					final int sendEndpointPort = message.getSendEndpointPort();
					final String sendEndpointToken = message.getSendEndpointToken();
					if(	receiveEndpointAddress.isEmpty() || 
						receiveEndpointPort == 0 ||
						receiveEndpointToken.isEmpty() ||
						sendEndpointAddress.isEmpty() || 
						sendEndpointPort == 0 || 
						sendEndpointToken.isEmpty() || 
						message.getUsername().isEmpty()) {
						Log.d(TAG, String.format("%s received but was malformed", message.getClass().getSimpleName()));
						return;
					}
					BCConstants.STREAM_SERVER_SEND_HOST = sendEndpointAddress;
					BCConstants.STREAM_SERVER_SEND_PORT = sendEndpointPort;
					BCConstants.STREAM_SERVER_SEND_TOKEN = sendEndpointToken;
					BCConstants.STREAM_SERVER_RCV_HOST = receiveEndpointAddress;
					BCConstants.STREAM_SERVER_RCV_PORT = receiveEndpointPort;
					BCConstants.STREAM_SERVER_RCV_TOKEN = receiveEndpointToken;
					SetupStreamingAsyncTask.notifyHasStreamServerInformation();
				}
			};
}
