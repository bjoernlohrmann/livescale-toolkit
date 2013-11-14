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
 * @author louis
 *
 */
public class ClientFavoriteStreamStarted extends AbstractMessage {
	
	private static final String MAP_KEY_RECEIVE_TOKEN = "MAP_KEY_RECEIVE_TOKEN";
	private static final String MAP_KEY_USERNAME = "MAP_KEY_USERNAME";
	
	private String username; 
	private String receiveToken;
	
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setUsername((String) messageMap.get(MAP_KEY_USERNAME));
		this.setReceiveToken((String) messageMap.get(MAP_KEY_RECEIVE_TOKEN));
	}
	
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_USERNAME, this.getUsername());
		messageMap.put(MAP_KEY_RECEIVE_TOKEN, this.getReceiveToken());
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
