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

import de.tuberlin.cit.livescale.messaging.AbstractMessage;

/**
 * 
 * @author Bernd Louis
 */
public class DispatcherStreamClose extends AbstractMessage {
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN = "MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN";
	
	private String sendToken;
	private String receiveToken;
	
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setSendToken((String) messageMap.get(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN));
		this.setReceiveToken((String) messageMap.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
	}
	
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_SEND_ENDPOINT_TOKEN, this.getSendToken());
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN, this.getReceiveToken());
	}

	/**
	 * Returns the sendToken.
	 * 
	 * @return the sendToken
	 */
	public String getSendToken() {
		return this.sendToken;
	}

	/**
	 * Sets the sendToken to the specified value.
	 *
	 * @param sendToken the sendToken to set
	 */
	public void setSendToken(String sendToken) {
		if(sendToken == null) 
			throw new NullPointerException("sendToken must not be null");
		
		this.sendToken = sendToken;
	}

	/**
	 * Returns the receiveToken.
	 * 
	 * @return the receiveToken
	 */
	public String getReceiveToken() {
		return this.receiveToken;
	}

	/**
	 * Sets the receiveToken to the specified value.
	 *
	 * @param receiveToken the receiveToken to set
	 */
	public void setReceiveToken(String receiveToken) {
		if(receiveToken == null) 
			throw new NullPointerException("receiveToken must not be null");
		
		this.receiveToken = receiveToken;
	}
}
