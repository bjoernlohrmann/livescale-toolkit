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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * This endpoint represents the endpoint for <a
 * href="http://developer.android.com/google/gcm/index.html">GCM</a>
 * 
 * URI format for this endpoint follows the format gcm://google/userKey
 * 
 * TODO implementation of multicast messages ({@link MessageManifest}) 
 * 
 * URI-Scheme: gcm://[instanceName]/receiverKey
 * 
 * @author Bernd Louis
 */
public class GCMEndpoint extends AbstractMessageEndpoint {
	/**
	 * Logger
	 */
	private static final Log LOG = LogFactory.getLog(GCMEndpoint.class);
	
	private static final String CONFIG_KEY_API_KEY = "apiKey";
	private static final String SCHEME = "gcm";

	private static final String[] requiredConfigFields = { CONFIG_KEY_API_KEY };

	private Collection<MessageEndpointListener> messageEndpointListeners = new LinkedList<MessageEndpointListener>();

	private String apiKey;
	private Sender sender;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#configure
	 * (java.util.Map)
	 */
	@Override
	public void configure(Map<String, String> properties) {
		for (String s : requiredConfigFields) {
			if (!properties.containsKey(s)) {
				throw new IllegalArgumentException(String.format(
						"Configuration key missing: %s", s));
			}
		}
		this.apiKey = properties.get(CONFIG_KEY_API_KEY);
		this.sender = new Sender(this.apiKey);
	}
	
	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performSend(de.tuberlin.cit.livescale.messaging.MessageManifest)
	 */
	@Override
	protected void performSend(MessageManifest manifest) {
		for (de.tuberlin.cit.livescale.messaging.Message m : manifest
				.getMessages()) {
			try {
				Result result = this.sender.send(this.createMessage(m),
						manifest.getTargetURI().getPath().substring(1), 5);
				// let's check for side conditions ...
				if (result.getMessageId() != null) {
					String canonicalRegId = result.getCanonicalRegistrationId();
					if (canonicalRegId != null) { // user has a new address
						URI localURI = getLocalEndpointURI();
						URI newTargetURI = new URI(localURI.getScheme(), localURI.getAuthority(), canonicalRegId, "", "");
						notifyListenersDeliveryEvent(new TargetMovedSuccess(manifest, newTargetURI));

					}
				} 
				else { // User's gone.
					String error = result.getErrorCodeName();
					if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
						notifyListenersDeliveryEvent(new TargetMovedFailure(manifest));
					}
				}
			} catch (IOException e) {
				notifyListenersDeliveryEvent(new ServiceUnavailableFailure(manifest));
			} catch (URISyntaxException e) { 
				LOG.debug("Error building a new URI from the local one. THIS SHOULD NEVER HAPPEN!", e);
			}
		}
	}


	/**
	 * Converts a "regular" livescale-messaging <code>Message</code> to a
	 * {@link Message}
	 * 
	 * @param msg
	 *            a {@link de.tuberlin.cit.livescale.messaging.Message}
	 * @return a GCM-{@link Message}
	 */
	private Message createMessage(
			de.tuberlin.cit.livescale.messaging.Message msg) {
		Map<String, Object> outmap = new HashMap<String, Object>();
		msg.toMap(outmap);
		Message.Builder mb = new Message.Builder();
		for (Map.Entry<String, Object> entry : outmap.entrySet()) {
			mb.addData(entry.getKey(), String.valueOf(entry.getValue()));
		}
		return mb.build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#
	 * addMessageEndpointListener
	 * (de.tuberlin.cit.livescale.messaging.endpoints
	 * .MessageEndpointListener)
	 */
	@Override
	public void addMessageEndpointListener(
			MessageEndpointListener messageEndpointListener) {
		this.messageEndpointListeners.add(messageEndpointListener);
	}


	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performStart()
	 */
	@Override
	protected void performStart() {
		// no particular startup required
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performShutdown()
	 */
	@Override
	protected void performShutdown() {
		// no particular shutdown required
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#getScheme()
	 */
	@Override
	protected String getScheme() {
		return SCHEME;
	}
}
