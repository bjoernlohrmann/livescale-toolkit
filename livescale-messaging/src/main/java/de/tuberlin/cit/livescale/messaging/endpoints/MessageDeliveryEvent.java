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
package de.tuberlin.cit.livescale.messaging.endpoints;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * 
 * @author Bernd Louis
 *
 */
public abstract class MessageDeliveryEvent {
	private MessageManifest messageManifest;

	public MessageDeliveryEvent(MessageManifest messageManifest) {
		this.messageManifest = messageManifest;
	}

	/**
	 * Returns the messageManifest.
	 * 
	 * @return the messageManifest
	 */
	public MessageManifest getMessageManifest() {
		return this.messageManifest;
	}

	/**
	 * Sets the messageManifest to the specified value.
	 *
	 * @param messageManifest the messageManifest to set
	 */
	public void setMessageManifest(MessageManifest messageManifest) {
		if(messageManifest == null) 
			throw new NullPointerException("messageManifest must not be null");
		
		this.messageManifest = messageManifest;
	}
}
