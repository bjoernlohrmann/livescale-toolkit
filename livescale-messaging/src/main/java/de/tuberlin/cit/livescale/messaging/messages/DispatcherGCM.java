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
 * {@link Message} sent by the client to the dispatcher (via broadcast) to
 * announce its <code>RegistrationID</code> from google's C2DM service.
 * 
 * This allows the dispatcher to use c2dm so send notifications directly to the
 * client.
 * 
 * @author Bernd Louis
 */
public class DispatcherGCM extends AbstractMessage {

	private static final String MAP_KEY_USER_C2DM = "MAP_KEY_USER_C2DM";
	private static final String MAP_KEY_USER_PASSWORD = "MAP_KEY_USER_PASSWORD";
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";

	private String username;
	private String password;
	private String c2dmKey;

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
		this.setC2dmKey((String) messageMap.get(MAP_KEY_USER_C2DM));
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
		messageMap.put(MAP_KEY_USER_C2DM, this.getC2dmKey());
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

	/**
	 * Returns the c2dmKey.
	 * 
	 * @return the c2dmKey
	 */
	public String getC2dmKey() {
		return this.c2dmKey;
	}

	/**
	 * Sets the c2dmKey to the specified value.
	 * 
	 * @param c2dmKey
	 *            the c2dmKey to set
	 */
	public void setC2dmKey(String c2dmKey) {
		if (c2dmKey == null) {
			throw new NullPointerException("c2dmKey must not be null");
		}

		this.c2dmKey = c2dmKey;
	}

}
