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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import de.tuberlin.cit.livescale.messaging.AbstractMessage;
import de.tuberlin.cit.livescale.messaging.Message;
import de.tuberlin.cit.livescale.messaging.MessageFactory;
import de.tuberlin.cit.livescale.messaging.MessageManifest;
import de.tuberlin.cit.livescale.messaging.ResponseMessage;
import de.tuberlin.cit.livescale.messaging.MessageFactory.InvalidMessageTypeException;
import de.tuberlin.cit.livescale.messaging.MessageFactory.MalformedMessageException;
import de.tuberlin.cit.livescale.messaging.messages.ClientFavoriteStreamStarted;
import de.tuberlin.cit.livescale.messaging.messages.ClientFollowerAnswer;
import de.tuberlin.cit.livescale.messaging.messages.ClientRegistrationAnswer;
import de.tuberlin.cit.livescale.messaging.messages.ClientStreamRcv;
import de.tuberlin.cit.livescale.messaging.messages.ClientStreamSend;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherGCM;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRegistration;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestFollower;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestStreamRcv;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherRequestStreamSend;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamConfirm;
import de.tuberlin.cit.livescale.messaging.messages.DispatcherStreamStatus;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverNewStream;
import de.tuberlin.cit.livescale.messaging.messages.StreamserverRequestStreamStatus;
import de.tuberlin.cit.livescale.messaging.messages.TestMessage;

/**
 * This collection tests the serialization process on the message classes
 * 
 * @author Bernd Louis
 * 
 */
public class MessagesSerializationTests {

	private UUID uuid;

	@Before
	public void setUp() {
		this.uuid = UUID.randomUUID();
	}

	/**
	 * Simulates marshaling through the {@link MessageFactory} by first
	 * rendering the {@link Message} and then trying to create a new one from
	 * the provided {@link Map}
	 * 
	 * @param m
	 * @return
	 * @throws InvalidMessageTypeException
	 *             in case the {@link MessageFactory} was not able to resolve
	 *             the Message
	 */
	private Message renderAndCreate(Message m)
			throws InvalidMessageTypeException {
		return MessageFactory.createMessage(MessageFactory.renderMessage(m));
	}

	/**
	 * Asserts the equality of <code>m1</code> and <code>m2</code>'s Message
	 * UUIDs.
	 * 
	 * @param m1
	 * @param m2
	 */
	private void assertUUIDEquality(Message m1, Message m2) {
		assertEquals(m1.getUUID(), m2.getUUID());
		if (m1 instanceof ResponseMessage && m2 instanceof ResponseMessage) {
			this.assertResponseRequestUUIDEquality((ResponseMessage) m1,
					(ResponseMessage) m2);
		}
	}

	/**
	 * Asserts the equality of of <code>m1</code> and <code>m2</code>'s request
	 * UUIDs.
	 * 
	 * @param m1
	 * @param m2
	 */
	private void assertResponseRequestUUIDEquality(ResponseMessage m1,
			ResponseMessage m2) {
		assertEquals(m1.getRequestMessageUUID(), m2.getRequestMessageUUID());
	}

	@Test
	public void testDispatcherGCM() throws InvalidMessageTypeException {
		DispatcherGCM m1 = new DispatcherGCM();
		m1.setUsername("uname");
		m1.setPassword("password");
		m1.setC2dmKey("key");
		DispatcherGCM m2 = (DispatcherGCM) this.renderAndCreate(m1);
		assertEquals(m1.getUsername(), m2.getUsername());
		assertEquals(m1.getPassword(), m2.getPassword());
		assertEquals(m1.getC2dmKey(), m2.getC2dmKey());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherRegistrationSerialization()
			throws InvalidMessageTypeException {
		DispatcherRegistration m1 = new DispatcherRegistration();
		m1.setUsername("uname");
		m1.setPassword("pwd");
		DispatcherRegistration m2 = (DispatcherRegistration) this
				.renderAndCreate(m1);
		assertEquals(m1.getUsername(), m2.getUsername());
		assertEquals(m1.getPassword(), m2.getPassword());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherRequestFollower()
			throws InvalidMessageTypeException {
		DispatcherRequestFollower m1 = new DispatcherRequestFollower();
		m1.setFollowerUsername("uname1");
		m1.setFollowRequest(-1);
		m1.setPassword("pwd");
		m1.setUsername("uname2");
		DispatcherRequestFollower m2 = (DispatcherRequestFollower) this
				.renderAndCreate(m1);
		assertEquals(m1.getFollowerUsername(), m2.getFollowerUsername());
		assertEquals(m1.getFollowRequest(), m2.getFollowRequest());
		assertEquals(m1.getPassword(), m2.getPassword());
		assertEquals(m1.getUsername(), m2.getUsername());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherRequestStreamRcv()
			throws InvalidMessageTypeException {
		DispatcherRequestStreamRcv m1 = new DispatcherRequestStreamRcv();
		m1.setToken("token");
		DispatcherRequestStreamRcv m2 = (DispatcherRequestStreamRcv) this
				.renderAndCreate(m1);
		assertEquals(m1.getToken(), m2.getToken());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherRequestStreamSend()
			throws InvalidMessageTypeException {
		DispatcherRequestStreamSend m1 = new DispatcherRequestStreamSend();
		m1.setUsername("uname");
		m1.setPassword("pwd");
		DispatcherRequestStreamSend m2 = (DispatcherRequestStreamSend) this
				.renderAndCreate(m1);
		assertEquals(m1.getUsername(), m2.getUsername());
		assertEquals(m1.getPassword(), m2.getPassword());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testStreamServerNewStream() throws InvalidMessageTypeException {
		StreamserverNewStream m1 = new StreamserverNewStream();
		m1.setReceiveEndpointToken("rectoken");
		m1.setSendEndpointToken("stoken");
		m1.setUsername("uname");
		StreamserverNewStream m2 = (StreamserverNewStream) this
				.renderAndCreate(m1);
		assertEquals(m1.getReceiveEndpointToken(), m2.getReceiveEndpointToken());
		assertEquals(m1.getSendEndpointToken(), m2.getSendEndpointToken());
		assertEquals(m1.getUsername(), m2.getUsername());
		assertUUIDEquality(m1, m2);
	}

	@Test
	public void testStreamserverRequestStreamStatus()
			throws InvalidMessageTypeException {
		StreamserverRequestStreamStatus m1 = new StreamserverRequestStreamStatus();
		m1.setReceiveEndpointToken("recep");
		m1.setSendEndpointToken("sendep");
		StreamserverRequestStreamStatus m2 = (StreamserverRequestStreamStatus) this
				.renderAndCreate(m1);
		assertEquals(m1.getReceiveEndpointToken(), m2.getReceiveEndpointToken());
		assertEquals(m1.getSendEndpointToken(), m2.getSendEndpointToken());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testClientFollowerAnswer() throws InvalidMessageTypeException {
		ClientFollowerAnswer m1 = new ClientFollowerAnswer(this.uuid);
		m1.setFollowerName("fname");
		m1.setFollowerResult(-1);
		m1.setUsername("uname");
		ClientFollowerAnswer m2 = (ClientFollowerAnswer) this
				.renderAndCreate(m1);
		assertEquals(m1.getFollowerName(), m2.getFollowerName());
		assertEquals(m1.getFollowerResult(), m2.getFollowerResult());
		assertEquals(m1.getUsername(), m2.getUsername());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testClientRegistrationAnswer()
			throws InvalidMessageTypeException {
		ClientRegistrationAnswer m1 = new ClientRegistrationAnswer(this.uuid);
		m1.setErrorMessage("errormsg");
		m1.setOk(true);
		m1.setUsername("uname");
		ClientRegistrationAnswer m2 = (ClientRegistrationAnswer) this
				.renderAndCreate(m1);
		assertEquals(m1.getErrorMessage(), m2.getErrorMessage());
		assertEquals(m1.isOk(), m2.isOk());
		assertEquals(m1.getUsername(), m2.getUsername());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testClientStreamRcv() throws InvalidMessageTypeException {
		ClientStreamRcv m1 = new ClientStreamRcv(this.uuid);
		m1.setAddress("address");
		m1.setPort(2342);
		m1.setToken("token");
		m1.setUserNameSender("uamesender");
		ClientStreamRcv m2 = (ClientStreamRcv) this.renderAndCreate(m1);
		assertEquals(m1.getAddress(), m2.getAddress());
		assertEquals(m1.getPort(), m2.getPort());
		assertEquals(m1.getToken(), m2.getToken());
		assertEquals(m1.getUserNameSender(), m2.getUserNameSender());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testClientStreamSend() throws InvalidMessageTypeException {
		ClientStreamSend m1 = new ClientStreamSend(this.uuid);
		m1.setReceiveEndpointAddress("recep");
		m1.setReceiveEndpointPort(2342);
		m1.setReceiveEndpointToken("rectoken");
		m1.setSendEndpointAddress("sendep");
		m1.setSendEndpointPort(4269);
		m1.setSendEndpointToken("stoken");
		m1.setUsername("uname");
		ClientStreamSend m2 = (ClientStreamSend) this.renderAndCreate(m1);
		assertEquals(m1.getReceiveEndpointAddress(),
				m2.getReceiveEndpointAddress());
		assertEquals(m1.getReceiveEndpointPort(), m2.getReceiveEndpointPort());
		assertEquals(m1.getReceiveEndpointToken(), m2.getReceiveEndpointToken());
		assertEquals(m1.getSendEndpointAddress(), m2.getSendEndpointAddress());
		assertEquals(m1.getSendEndpointPort(), m2.getSendEndpointPort());
		assertEquals(m1.getSendEndpointToken(), m2.getSendEndpointToken());
		assertEquals(m1.getUsername(), m2.getUsername());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherStreamConfirm()
			throws InvalidMessageTypeException {
		DispatcherStreamConfirm m1 = new DispatcherStreamConfirm(this.uuid);
		m1.setReceiveEndpointAddress("recep");
		m1.setReceiveEndpointPort(2342);
		m1.setReceiveEndpointToken("rectoken");
		m1.setSendEndpointAddress("sendep");
		m1.setSendEndpointPort(4269);
		m1.setSendEndpointToken("stoken");
		m1.setUsername("usern");
		DispatcherStreamConfirm m2 = (DispatcherStreamConfirm) this
				.renderAndCreate(m1);
		assertEquals(m1.getReceiveEndpointAddress(),
				m2.getReceiveEndpointAddress());
		assertEquals(m1.getReceiveEndpointPort(), m2.getReceiveEndpointPort());
		assertEquals(m1.getReceiveEndpointToken(), m2.getReceiveEndpointToken());
		assertEquals(m1.getSendEndpointAddress(), m2.getSendEndpointAddress());
		assertEquals(m1.getSendEndpointPort(), m2.getSendEndpointPort());
		assertEquals(m1.getSendEndpointToken(), m2.getSendEndpointToken());
		assertEquals(m1.getUsername(), m2.getUsername());
		this.assertUUIDEquality(m1, m2);
	}

	@Test
	public void testDispatcherStreamStatus() throws InvalidMessageTypeException {
		DispatcherStreamStatus m1 = new DispatcherStreamStatus(this.uuid);
		m1.setActive(true);
		m1.setReceiveEndpointToken("recep");
		m1.setSendEndpointToken("seep");
		DispatcherStreamStatus m2 = (DispatcherStreamStatus) this
				.renderAndCreate(m1);
		assertEquals(m1.isActive(), m2.isActive());
		assertEquals(m1.getReceiveEndpointToken(), m2.getReceiveEndpointToken());
		assertEquals(m1.getSendEndpointToken(), m2.getSendEndpointToken());
		this.assertUUIDEquality(m1, m2);
	}
	
	@Test
	public void testClientFavoriteStreamStarted() throws InvalidMessageTypeException {
		ClientFavoriteStreamStarted m1 = new ClientFavoriteStreamStarted();
		m1.setUsername("leName");
		m1.setReceiveToken("recToken");
		ClientFavoriteStreamStarted m2 = (ClientFavoriteStreamStarted)
				this.renderAndCreate(m1);
		assertEquals(m1.getUsername(), m2.getUsername());
		assertEquals(m1.getReceiveToken(), m2.getReceiveToken());
	}

	@Test(expected = MalformedMessageException.class)
	public void testFailOnMissingAbstractMessageToMapSuperCall() {
		AbstractMessage am = new AbstractMessage() {
			@Override
			public void toMap(Map<String, Object> messageMap) {
				// we're not doing a super call here! so the UUID field
				// will be missing
			}
		};
		MessageFactory.renderMessage(am);
	}

	@Test(expected = MalformedMessageException.class)
	public void testFailOnResponseMessageMissingToMapSuperCall() {
		ResponseMessage rm = new ResponseMessage(UUID.randomUUID()) {
			@Override
			public void toMap(Map<String, Object> messageMap) {
				// as of this writing this is the
				// same situation as the
				// testFailOnMissingAbstractMessageToMapSuperCall
				// Test, but this might change in the future. So we might write
				// a
				// test for this just as well.
			}
		};
		MessageFactory.renderMessage(rm);
	}
	
	@Test
	public void testDirectMessageManifestSerialization() throws URISyntaxException, InvalidMessageTypeException {
		TestMessage m1 = new TestMessage();
		m1.setFieldFive("m1.5");
		m1.setFieldSix("m1.6");
		TestMessage m2 = new TestMessage();
		m2.setFieldFive("m2.5");
		m2.setFieldSix("m2.6");
		Collection<Message> msgs = new LinkedList<Message>();
		msgs.add(m1);
		msgs.add(m2);
		MessageManifest mm1 = new MessageManifest(msgs, new URI("testscheme", "testauthority", "/testpath", "", ""));
		Map<String, Object> outmap = new HashMap<String, Object>();
		mm1.toMap(outmap);
		MessageManifest mm2 = new MessageManifest(outmap);
		assertEquals(mm1.getTargetURI(), mm2.getTargetURI());
		assertTrue(mm2.getMessages().size() == 2);
	}
	
	@Test
	public void testIndirectMessageManifestSerialization() throws URISyntaxException, MalformedMessageException, IOException {
		TestMessage m1 = new TestMessage();
		String m1five = "m1.5";
		m1.setFieldFive(m1five);
		String m1six = "m1.6";
		m1.setFieldSix(m1six);
		TestMessage m2 = new TestMessage();
		String m2five = "m2.5";
		String m2six = "m2.6";
		m2.setFieldFive(m2five);
		m2.setFieldSix(m2six);
		Collection<Message> msgs = new LinkedList<Message>();
		msgs.add(m1);
		msgs.add(m2);
		MessageManifest mm1 = new MessageManifest(msgs, new URI("testscheme", "testauthority", "/testpath", "", ""));
		MessageManifest mm2 = MessageFactory.decode(MessageFactory.encode(mm1));
		assertEquals(mm1.getTargetURI(), mm2.getTargetURI());
		assertTrue(mm2.getMessages().size() == 2);
		Iterator<Message> it = mm2.getMessages().iterator();
		TestMessage out1 = (TestMessage) it.next();
		TestMessage out2 = (TestMessage) it.next();
		assertEquals(m1five, out1.getFieldFive());
		assertEquals(m1six, out1.getFieldSix());
		assertEquals(m2five, out2.getFieldFive());
		assertEquals(m2six, out2.getFieldSix());
	}
}
