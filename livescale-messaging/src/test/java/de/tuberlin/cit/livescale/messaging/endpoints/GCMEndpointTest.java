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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.endpoints.GCMEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint;

/**
 * @author louis
 * 
 */
public class GCMEndpointTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyConfigurationFail() {
		new GCMEndpoint().configure(new HashMap<String, String>());
	}

	@Test
	public void testInferedConfiguration() throws IOException,
			URISyntaxException {
		// Note: like all "Infered" Tests this relies on the test-configuration
		// located
		// in src/test/resources
		MessageCenter mc = new MessageCenter(false);
		Collection<MessageEndpoint> eps = Whitebox.getInternalState(mc,
				"messageEndpoints");
		boolean hasGCMEndpoint = false;
		for (MessageEndpoint ep : eps) {
			if (ep instanceof GCMEndpoint) {
				hasGCMEndpoint = true;
				break;
			}
		}
		assertTrue(
				"MessageCenter should have an instanceof GCMEndpoint in its endpoints",
				hasGCMEndpoint);
		GCMEndpoint gcmEndpoint = new GCMEndpoint();
		gcmEndpoint.setName("myGcm");
		assertTrue("Endpoint should answer to its own authority", 
				mc.hasEndpointToURI(gcmEndpoint.getLocalEndpointURI()));
		assertTrue("GCM-Endpoint should answer to any authority", 
				mc.hasEndpointToURI(new URI(gcmEndpoint.getLocalEndpointURI().getScheme(),
				"myGcm", "", "", "")));
	}

}
