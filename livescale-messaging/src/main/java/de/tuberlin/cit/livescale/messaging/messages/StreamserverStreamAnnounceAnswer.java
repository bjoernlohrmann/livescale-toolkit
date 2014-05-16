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
 * @author bjoern
 * 
 */
public class StreamserverStreamAnnounceAnswer extends ResponseMessage {

	/**
	 * Initializes StreamserverStreamAnnounceAnswer.
	 *
	 * @param requestMessageUUID
	 */
	public StreamserverStreamAnnounceAnswer(UUID requestMessageUUID) {
		super(requestMessageUUID);
	}

	private static final String MAP_KEY_STREAM_ID = "MAP_KEY_STREAM_ID";
	private static final String MAP_KEY_GROUP_ID = "MAP_KEY_GROUP_ID";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN = "MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_URL = "MAP_KEY_STREAM_RCV_ENDPOINT_URL";

	private long streamId;

	private long groupId;

	private String sendEndpointToken;

	private String receiveEndpointUrl;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#fromMap(java.util.Map)
	 */
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setReceiveEndpointUrl((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_URL));
		this.setSendEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN));
		this.setStreamId((long) messageMap.get(MAP_KEY_STREAM_ID));
		this.setGroupId((long) messageMap.get(MAP_KEY_GROUP_ID));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_URL,
				this.getReceiveEndpointUrl());
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN,
				this.getSendEndpointToken());
		messageMap.put(MAP_KEY_STREAM_ID, this.getStreamId());
		messageMap.put(MAP_KEY_GROUP_ID, this.getGroupId());
	}

	/**
	 * Returns the streamId.
	 * 
	 * @return the streamId
	 */
	public long getStreamId() {
		return this.streamId;
	}

	/**
	 * Sets the streamId to the specified value.
	 * 
	 * @param streamId
	 *            the streamId to set
	 */
	public void setStreamId(long streamId) {
		this.streamId = streamId;
	}

	/**
	 * Returns the groupId.
	 * 
	 * @return the groupId
	 */
	public long getGroupId() {
		return this.groupId;
	}

	/**
	 * Sets the groupId to the specified value.
	 * 
	 * @param groupId
	 *            the groupId to set
	 */
	public void setGroupId(long groupId) {

		this.groupId = groupId;
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
		if (sendEndpointToken == null)
			throw new NullPointerException("sendEndpointToken must not be null");

		this.sendEndpointToken = sendEndpointToken;
	}

	/**
	 * Returns the receiveEndpointUrl.
	 * 
	 * @return the receiveEndpointUrl
	 */
	public String getReceiveEndpointUrl() {
		return this.receiveEndpointUrl;
	}

	/**
	 * Sets the receiveEndpointUrl to the specified value.
	 * 
	 * @param receiveEndpointUrl
	 *            the receiveEndpointUrl to set
	 */
	public void setReceiveEndpointUrl(String receiveEndpointUrl) {
		if (receiveEndpointUrl == null)
			throw new NullPointerException(
					"receiveEndpointUrl must not be null");

		this.receiveEndpointUrl = receiveEndpointUrl;
	}
}
