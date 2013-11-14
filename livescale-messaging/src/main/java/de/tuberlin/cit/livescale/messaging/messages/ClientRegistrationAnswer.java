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
 * {@link ResponseMessage} to {@link DispatcherRegistration} announcing
 * 
 * This announces the result of a registration request from the dispatcher to
 * the client
 * 
 * @author Bernd Louis
 * 
 */
public class ClientRegistrationAnswer extends ResponseMessage {

	private static final String MAP_KEY_USER_REGISTRATION_OK = "MAP_KEY_USER_REGISTRATION_OK";
	private static final String MAP_KEY_USER_REGISTRATION_ERROR_MSG = "MAP_KEY_USER_REGISTRATION_ERROR_MSG";
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";

	private String username;
	private String errorMessage;
	private boolean ok;

	/**
	 * Initializes ClientRegistrationAnswer.
	 * 
	 * @param requestMessageUUID
	 */
	public ClientRegistrationAnswer(UUID requestMessageUUID) {
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
		this.setUsername((String) messageMap.get(MAP_KEY_USER_NAME));
		this.setErrorMessage((String) messageMap
				.get(MAP_KEY_USER_REGISTRATION_ERROR_MSG));
		this.setOk(Boolean.parseBoolean((String) messageMap
				.get(MAP_KEY_USER_REGISTRATION_OK)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_USER_NAME, this.getUsername());
		messageMap.put(MAP_KEY_USER_REGISTRATION_ERROR_MSG,
				this.getErrorMessage());
		messageMap.put(MAP_KEY_USER_REGISTRATION_OK,
				String.valueOf(this.isOk()));
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
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		if (username == null) {
			throw new NullPointerException("username must not be null");
		}

		this.username = username;
	}

	/**
	 * Returns the errorMessage.
	 * 
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return this.errorMessage;
	}

	/**
	 * Sets the errorMessage to the specified value.
	 * 
	 * @param errorMessage
	 *            the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		if (errorMessage == null) {
			throw new NullPointerException("errorMessage must not be null");
		}

		this.errorMessage = errorMessage;
	}

	/**
	 * Returns if the registration was successful.
	 * 
	 * @return the ok
	 */
	public boolean isOk() {
		return this.ok;
	}

	/**
	 * Sets if the registration was successful.
	 * 
	 * @param the
	 *            success-state
	 */
	public void setOk(boolean ok) {

		this.ok = ok;
	}

}
