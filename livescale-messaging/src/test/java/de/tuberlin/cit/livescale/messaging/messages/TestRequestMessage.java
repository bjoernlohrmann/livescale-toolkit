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
 * @author louis
 * 
 */
public class TestRequestMessage extends RequestMessage {

	/**
	 * 
	 */
	private static final String FIELD_TWO = "FIELD_TWO";
	/**
	 * 
	 */
	private static final String FIELD_ONE = "FIELD_ONE";
	private String fieldOne, fieldTwo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.Message#fromMap(java.util.Map)
	 */
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.fieldOne = (String) messageMap.get(FIELD_ONE);
		this.fieldTwo = (String) messageMap.get(FIELD_TWO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap()
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(FIELD_ONE, this.fieldOne);
		messageMap.put(FIELD_TWO, this.fieldTwo);
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
		return new TestResponseMessage(this.getUUID());
	}
}
