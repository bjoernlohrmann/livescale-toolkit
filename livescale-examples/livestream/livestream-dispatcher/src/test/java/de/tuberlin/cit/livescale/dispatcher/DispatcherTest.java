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
package de.tuberlin.cit.livescale.dispatcher;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import de.tuberlin.cit.livescale.dispatcher.Dispatcher;
import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint;

/**
 * @author louis
 *
 */
public class DispatcherTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testStart() throws IOException, URISyntaxException {
		Dispatcher d = new Dispatcher();
		MessageCenter mc = Whitebox.getInternalState(d, "messageCenter");
		Collection<MessageEndpoint> eps = Whitebox.getInternalState(mc, "messageEndpoints");
		assertTrue("Dispatcher's endpoint should at least the gcm endpoint configured", eps.size() > 0);
		assertTrue(mc.hasEndpointToURI(new URI("gcm", "myGcm", "", "", "")));
	}
}
