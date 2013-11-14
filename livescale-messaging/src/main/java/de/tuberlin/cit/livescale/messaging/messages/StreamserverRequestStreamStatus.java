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

import de.tuberlin.cit.livescale.messaging.RequestMessage;
import de.tuberlin.cit.livescale.messaging.ResponseMessage;

/**
 * {@link RequestMessage} broadcast to all stream servers to check if a given
 * stream is still active.
 * 
 * The corresponding {@link ResponseMessage} is {@link DispatcherStreamStatus}
 * message.
 * 
 * @author Bernd Louis
 * 
 */
public class StreamserverRequestStreamStatus extends RequestMessage {

	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN = "MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN";

	private String sendEndpointToken;
	private String receiveEndpointToken;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.Message#fromMap(java.util.Map)
	 */
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setSendEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN));
		this.setReceiveEndpointToken((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN,
				this.getSendEndpointToken());
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN,
				this.getReceiveEndpointToken());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.RequestMessage#getResponseMessage
	 * ()
	 */
	@Override
	public ResponseMessage getResponseMessage() {
		return new DispatcherStreamStatus(this.getUUID());
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
}
