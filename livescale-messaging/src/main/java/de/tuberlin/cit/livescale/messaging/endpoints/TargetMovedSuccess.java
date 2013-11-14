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

import java.net.URI;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * Indicates that a Message was successfully delivered, but the 
 * actual target address has changed. Thus the client should update
 * its database. 
 * 
 * @author Bernd Louis
 *
 */
public class TargetMovedSuccess extends MessageDeliveryEvent implements
		DeliverySuccess {
	
	private URI newTargetURI;
	
	/**
	 * Initializes TargetMovedSuccess.
	 *
	 * @param messageManifest
	 */
	public TargetMovedSuccess(MessageManifest messageManifest, URI newTargetURI) {
		super(messageManifest);
		this.newTargetURI = newTargetURI;
	}

	/**
	 * Returns the newTargetURI.
	 * 
	 * @return the newTargetURI
	 */
	public URI getNewTargetURI() {
		return this.newTargetURI;
	}
}
