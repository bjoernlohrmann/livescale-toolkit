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
package de.tuberlin.cit.livescale.messaging.messages;

import java.util.Map;
import java.util.UUID;

import de.tuberlin.cit.livescale.messaging.ResponseMessage;

/**
 * {@link ResponseMessage} to {@link StreamserverRequestStreamStatus}.
 * 
 * The stream server announces the current state of the requested stream.
 * 
 * @author Bernd Louis
 * 
 */
public class DispatcherStreamStatus extends ResponseMessage {

	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN = "MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_ACTIVE = "MAP_KEY_STREAM_ACTIVE";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";

	private String receiveEndpointToken;
	private String sendEndpointToken;
	private boolean active;

	/**
	 * Initializes DispatcherStreamStatus.
	 * 
	 * @param requestMessageUUID
	 */
	public DispatcherStreamStatus(UUID requestMessageUUID) {
		super(requestMessageUUID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.Message#fromMap(java.util.Map)
	 */
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setReceiveEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
		this.setSendEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN));
		this.setActive(Boolean.valueOf((String) messageMap
				.get(MAP_KEY_STREAM_ACTIVE)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN,
				this.getReceiveEndpointToken());
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN,
				this.getSendEndpointToken());
		messageMap.put(MAP_KEY_STREAM_ACTIVE, String.valueOf(this.isActive()));
	}

	/**
	 * Returns the receiveEndpointToken.
	 * 
	 * @return the receiveEndpointToken
	 */
	public String getReceiveEndpointToken() {
		return this.receiveEndpointToken;
	}

	/**
	 * Sets the receiveEndpointToken to the specified value.
	 * 
	 * @param receiveEndpointToken
	 *            the receiveEndpointToken to set
	 */
	public void setReceiveEndpointToken(String receiveEndpointToken) {
		if (receiveEndpointToken == null) {
			throw new NullPointerException(
					"receiveEndpointToken must not be null");
		}

		this.receiveEndpointToken = receiveEndpointToken;
	}

	/**
	 * Returns the sendEndpointToken.
	 * 
	 * @return the sendEndpointToken
	 */
	public String getSendEndpointToken() {
		return this.sendEndpointToken;
	}

	/**
	 * Sets the sendEndpointToken to the specified value.
	 * 
	 * @param sendEndpointToken
	 *            the sendEndpointToken to set
	 */
	public void setSendEndpointToken(String sendEndpointToken) {
		if (sendEndpointToken == null) {
			throw new NullPointerException("sendEndpointToken must not be null");
		}

		this.sendEndpointToken = sendEndpointToken;
	}

	/**
	 * Returns the active.
	 * 
	 * @return the active
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * Sets the active to the specified value.
	 * 
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

}
