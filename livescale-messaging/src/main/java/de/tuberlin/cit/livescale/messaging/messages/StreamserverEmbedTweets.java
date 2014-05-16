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
 * @author bjoern
 *
 */
public class StreamserverEmbedTweets extends AbstractMessage {
	private static final String MAP_KEY_SEND_ENDPOINT_TOKEN = "MAP_KEY_SEND_ENDPOINT_TOKEN";
	private static final String MAP_KEY_TWITTER_FILTER_KEYWORD = "MAP_KEY_TWITTER_FILTER_KEYWORD";
	
	private String sendEndpointToken; 
	private String twitterFilterKeyword;
	
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setSendEndpointToken((String) messageMap.get(MAP_KEY_SEND_ENDPOINT_TOKEN));
		this.setTwitterFilterKeyword((String) messageMap.get(MAP_KEY_TWITTER_FILTER_KEYWORD));
	}
	
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_SEND_ENDPOINT_TOKEN, this.getSendEndpointToken());
		messageMap.put(MAP_KEY_TWITTER_FILTER_KEYWORD, this.getTwitterFilterKeyword());
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
	 * Returns the twitterFilterKeyword.
	 * 
	 * @return the twitterFilterKeyword
	 */
	public String getTwitterFilterKeyword() {
		return this.twitterFilterKeyword;
	}

	/**
	 * Sets the twitterFilterKeyword to the specified value.
	 *
	 * @param twitterFilterKeyword the twitterFilterKeyword to set
	 */
	public void setTwitterFilterKeyword(String twitterFilterKeyword) {
		if (twitterFilterKeyword == null)
			throw new NullPointerException("twitterFilterKeyword must not be null");
	
		this.twitterFilterKeyword = twitterFilterKeyword;
	}
}
