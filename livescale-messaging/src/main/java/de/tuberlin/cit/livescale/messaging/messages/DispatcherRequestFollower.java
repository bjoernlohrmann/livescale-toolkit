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
 * This {@link Message} encapsulates a client request to set or get the user's
 * follower status or to set one.
 * 
 * @author Bernd Louis
 * 
 */
public class DispatcherRequestFollower extends RequestMessage {
	private static final String MAP_KEY_USER_FOLLOW_REQUEST = "MAP_KEY_USER_FOLLOW_REQUEST";
	private static final String MAP_KEY_USER_FOLLOWER_NAME = "MAP_KEY_USER_FOLLOWER_NAME";
	private static final String MAP_KEY_USER_PASSWORD = "MAP_KEY_USER_PASSWORD";
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";

	private String username;
	private String password;
	private String followerUsername;
	/**
	 * -1 => unset follower 0 => get status 1 => set follower
	 */
	private int followRequest;

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
		this.setFollowerUsername((String) messageMap
				.get(MAP_KEY_USER_FOLLOWER_NAME));
		this.setFollowRequest(Integer.parseInt((String) messageMap
				.get(MAP_KEY_USER_FOLLOW_REQUEST)));
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
		messageMap.put(MAP_KEY_USER_FOLLOWER_NAME, this.getFollowerUsername());
		messageMap.put(MAP_KEY_USER_FOLLOW_REQUEST,
				String.valueOf(this.getFollowRequest()));
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
		return new ClientFollowerAnswer(this.getUUID());
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
	 * Returns the followerUsername.
	 * 
	 * @return the followerUsername
	 */
	public String getFollowerUsername() {
		return this.followerUsername;
	}

	/**
	 * Sets the followerUsername to the specified value.
	 * 
	 * @param followerUsername
	 *            the followerUsername to set
	 */
	public void setFollowerUsername(String followerUsername) {
		if (followerUsername == null) {
			throw new NullPointerException("followerUsername must not be null");
		}

		this.followerUsername = followerUsername;
	}

	/**
	 * Returns the followRequest.
	 * 
	 * -1 means not set, 1 means set
	 * 
	 * @return the followRequest
	 */
	public int getFollowRequest() {
		return this.followRequest;
	}

	/**
	 * Sets the followRequest to the specified value.
	 * 
	 * @param followRequest
	 *            the followRequest to set (-1 to unset, 0 to get the info, 1 to
	 *            set)
	 */
	public void setFollowRequest(int followRequest) {
		this.followRequest = followRequest;
	}
}
