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
 * Sends the response to a {@link DispatcherRequestFollower} message containing
 * the user name, the follower name and the current state (-1 no follower or 1
 * follower)
 * 
 * @author Bernd Louis
 * 
 */
public class ClientFollowerAnswer extends ResponseMessage {

	private static final String MAP_KEY_USER_FOLLOW_RESULT = "MAP_KEY_USER_FOLLOW_RESULT";
	private static final String MAP_KEY_USER_FOLLOWER_NAME = "MAP_KEY_USER_FOLLOWER_NAME";
	private static final String MAP_KEY_USER_NAME = "MAP_KEY_USER_NAME";

	private String username;
	private String followerName;
	private int followerResult;

	/**
	 * Initializes ClientFollowerAnswer.
	 * 
	 * @param requestMessageUUID
	 */
	public ClientFollowerAnswer(UUID requestMessageUUID) {
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
		this.setFollowerName((String) messageMap
				.get(MAP_KEY_USER_FOLLOWER_NAME));
		this.setFollowerResult(Integer.parseInt((String) messageMap
				.get(MAP_KEY_USER_FOLLOW_RESULT)));
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
		messageMap.put(MAP_KEY_USER_FOLLOWER_NAME, this.getFollowerName());
		messageMap.put(MAP_KEY_USER_FOLLOW_RESULT,
				String.valueOf(this.getFollowerResult()));
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
	 * Returns the followerName.
	 * 
	 * @return the followerName
	 */
	public String getFollowerName() {
		return this.followerName;
	}

	/**
	 * Sets the followerName to the specified value.
	 * 
	 * @param followerName
	 *            the followerName to set
	 */
	public void setFollowerName(String followerName) {
		if (followerName == null) {
			throw new NullPointerException("followerName must not be null");
		}

		this.followerName = followerName;
	}

	/**
	 * Returns the followerResult.
	 * 
	 * @return -1 no follow, 1 follow
	 */
	public int getFollowerResult() {
		return this.followerResult;
	}

	/**
	 * Sets the followerResult to the specified value.
	 * 
	 * @param followerResult
	 *            the followerResult to set (-1 no follow, 1 follow)
	 */
	public void setFollowerResult(int followerResult) {
		this.followerResult = followerResult;
	}

}
