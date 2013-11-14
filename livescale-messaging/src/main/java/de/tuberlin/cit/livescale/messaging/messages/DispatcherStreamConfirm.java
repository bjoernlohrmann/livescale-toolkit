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
 * {@link ResponseMessage} from the stream server to the dispatcher confirming
 * that a new endpoint is available to stream to.
 * 
 * The corresponding {@link RequestMessage} is {@link StreamserverNewStream}
 * 
 * @author Bernd Louis
 * 
 */
public class DispatcherStreamConfirm extends ResponseMessage {
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN = "MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_PORT = "MAP_KEY_STREAM_SEND_ENDPOINT_PORT";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_ADDRESS = "MAP_KEY_STREAM_SEND_ENDPOINT_ADDRESS";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_PORT = "MAP_KEY_STREAM_RCV_ENDPOINT_PORT";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS = "MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS";
	
	private String receiveEndpointAddress;
	private int receiveEndpointPort;
	private String receiveEndpointToken;
	private String sendEndpointAddress;
	private int sendEndpointPort;
	private String sendEndpointToken;
	private String username;
	
	/**
	 * Initializes DispatcherStreamConfirm.
	 * 
	 * @param requestMessageUUID
	 */
	public DispatcherStreamConfirm(UUID requestMessageUUID) {
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
		this.setReceiveEndpointAddress((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS));
		this.setReceiveEndpointPort(Integer.parseInt((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_PORT)));
		this.setReceiveEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
		this.setSendEndpointAddress((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_ADDRESS));
		this.setSendEndpointPort(Integer.parseInt((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_PORT)));
		this.setSendEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN));
		this.setUsername((String) messageMap.get(MAP_KEY_USER_NAME));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS,
				this.getReceiveEndpointAddress());
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_PORT,
				String.valueOf(this.getReceiveEndpointPort()));
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN,
				this.getReceiveEndpointToken());
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_ADDRESS,
				this.getSendEndpointAddress());
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_PORT,
				String.valueOf(this.getSendEndpointPort()));
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN,
				this.getSendEndpointToken());
		messageMap.put(MAP_KEY_USER_NAME, this.getUsername());
	}

	/**
	 * Returns the receiveEndpointAddress.
	 * 
	 * @return the receiveEndpointAddress
	 */
	public String getReceiveEndpointAddress() {
		return this.receiveEndpointAddress;
	}

	/**
	 * Sets the receiveEndpointAddress to the specified value.
	 * 
	 * @param receiveEndpointAddress
	 *            the receiveEndpointAddress to set
	 */
	public void setReceiveEndpointAddress(String receiveEndpointAddress) {
		if (receiveEndpointAddress == null) {
			throw new NullPointerException(
					"receiveEndpointAddress must not be null");
		}

		this.receiveEndpointAddress = receiveEndpointAddress;
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
	 * Returns the sendEndpointAddress.
	 * 
	 * @return the sendEndpointAddress
	 */
	public String getSendEndpointAddress() {
		return this.sendEndpointAddress;
	}

	/**
	 * Sets the sendEndpointAddress to the specified value.
	 * 
	 * @param sendEndpointAddress
	 *            the sendEndpointAddress to set
	 */
	public void setSendEndpointAddress(String sendEndpointAddress) {
		if (sendEndpointAddress == null) {
			throw new NullPointerException(
					"sendEndpointAddress must not be null");
		}

		this.sendEndpointAddress = sendEndpointAddress;
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
	 * Returns the receiveEndpointPort.
	 * 
	 * @return the receiveEndpointPort
	 */
	public int getReceiveEndpointPort() {
		return this.receiveEndpointPort;
	}

	/**
	 * Sets the receiveEndpointPort to the specified value.
	 * 
	 * @param receiveEndpointPort
	 *            the receiveEndpointPort to set
	 */
	public void setReceiveEndpointPort(int receiveEndpointPort) {
		this.receiveEndpointPort = receiveEndpointPort;
	}

	/**
	 * Returns the sendEndpointPort.
	 * 
	 * @return the sendEndpointPort
	 */
	public int getSendEndpointPort() {
		return this.sendEndpointPort;
	}

	/**
	 * Sets the sendEndpointPort to the specified value.
	 * 
	 * @param sendEndpointPort
	 *            the sendEndpointPort to set
	 */
	public void setSendEndpointPort(int sendEndpointPort) {
		this.sendEndpointPort = sendEndpointPort;
	}

	/**
	 * Returns the username.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username to the specified value.
	 *
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		if(username == null) 
			throw new NullPointerException("username must not be null");
		
		this.username = username;
	}
}
