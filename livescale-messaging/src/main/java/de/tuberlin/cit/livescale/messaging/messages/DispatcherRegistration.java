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
 * {@link RequestMessage} from client to dispatcher to register a new user
 * 
 * The corresponding {@link ResponseMessage} is {@link ClientRegistrationAnswer}
 * 
 * @author Bernd Louis
 * 
 */
public class DispatcherRegistration extends RequestMessage {

	private static final String MAP_KEY_USER_PASSWORD = "MAP_KEY_USER_PASSWORD";
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";

	private String username;
	private String password;

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
		this.setPassword((String) messageMap.get(MAP_KEY_USER_PASSWORD));
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
		messageMap.put(MAP_KEY_USER_PASSWORD, this.getPassword());
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
		return new ClientRegistrationAnswer(this.getUUID());
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
	 * Returns the password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password to the specified value.
	 * 
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		if (password == null) {
			throw new NullPointerException("password must not be null");
		}

		this.password = password;
	}

}
