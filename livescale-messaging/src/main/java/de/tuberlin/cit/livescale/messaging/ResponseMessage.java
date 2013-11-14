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
package de.tuberlin.cit.livescale.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Marks a Message as a response Message, can usually be created using a
 * {@link RequestMessage}
 * 
 * @author Bernd Louis
 * 
 */
public abstract class ResponseMessage extends AbstractMessage {
	protected static final String REQUEST_UUID = "__request_uuid";

	private UUID requestMessageUUID;

	public ResponseMessage(UUID requestMessageUUID) {
		this.requestMessageUUID = requestMessageUUID;
	}

	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.requestMessageUUID = UUID.fromString((String) messageMap
				.get(REQUEST_UUID));
	}

	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(REQUEST_UUID, this.requestMessageUUID.toString());
	}

	/**
	 * This is so the {@link MessageCenter} can figure out the way back
	 * 
	 * @return
	 */
	public UUID getRequestMessageUUID() {
		return this.requestMessageUUID;
	}
}