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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.MessageFactory;
import de.tuberlin.cit.livescale.messaging.MessageManifest;
import de.tuberlin.cit.livescale.messaging.endpoints.AMQPEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpointListener;
import de.tuberlin.cit.livescale.messaging.messages.TestMessage;
import de.tuberlin.cit.livescale.messaging.messages.TestRequestMessage;

/**
 * Tests for the {@link AMQPEndpoint}
 * 
 * @author Bernd Louis
 * 
 */
public class AMQPEndpointTest {

	private Map<String, String> exampleConf;
	private Properties emptyConf;
	
	@Before
	public void setUp() {
		this.exampleConf = new HashMap<String, String>();
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_BROKER_ADDRESS,
				"localhost");
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_BROKER_PORT,
				String.valueOf(2342));
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_ROUTING_KEY, "test");
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_LISTEN_FOR_TASKS,
				String.valueOf(true));
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_LISTEN_FOR_BROADCASTS,
				String.valueOf(true));
		this.exampleConf.put(AMQPEndpoint.CONFIG_KEY_EXCHANGE_NAME, "cit_stream_exchange");
		
		this.emptyConf = new Properties();
		this.emptyConf.setProperty("noConcurrency", String.valueOf(true));
		
	}

	@Test
	public void testConfiguration() {
		AMQPEndpoint ep = new AMQPEndpoint();
		ep.configure(this.exampleConf);

		this.<String> checkEquality(ep, AMQPEndpoint.CONFIG_KEY_BROKER_ADDRESS);
		this.<Integer> checkEquality(ep, AMQPEndpoint.CONFIG_KEY_BROKER_PORT);
		this.<String> checkEquality(ep, AMQPEndpoint.CONFIG_KEY_ROUTING_KEY);
		this.<Boolean> checkEquality(ep,
				AMQPEndpoint.CONFIG_KEY_LISTEN_FOR_TASKS);
		this.<Boolean> checkEquality(ep,
				AMQPEndpoint.CONFIG_KEY_LISTEN_FOR_BROADCASTS);
		this.<String> checkEquality(ep, AMQPEndpoint.CONFIG_KEY_EXCHANGE_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConfigurationFail() {
		new AMQPEndpoint().configure(new HashMap<String, String>());
	}

	/**
	 * This helper casts back from the original type <code>T</code> and checks
	 * if that equals the value in the example configuration.
	 * 
	 * ATTN: this will fail if the actual property names of {@link AMQPEndpoint}
	 * won't match their config keys. In that case you gotta check yourself
	 * using <code>assertEquals</code>
	 * 
	 * @param <T>
	 *            the type to try to cast to
	 * @param ep
	 *            the endpoint
	 * @param key
	 *            the config_key
	 */
	private <T> void checkEquality(AMQPEndpoint ep, String key) {
		assertEquals(this.exampleConf.get(key), String.valueOf(Whitebox
				.<T> getInternalState(ep, key, AMQPEndpoint.class)));
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
		boolean hasAMQPEndpoint = false;
		for (MessageEndpoint ep : eps) {
			if (ep instanceof AMQPEndpoint) {
				hasAMQPEndpoint = true;
				break;
			}
		}
		assertTrue(
				"MessageCenter should have an instanceof AMQPEndpoint in its endpoints",
				hasAMQPEndpoint);
		assertTrue(mc.hasEndpointToURI(new URI("amqp", "myRabbit", "", "")));
	}
	
	@Test(expected = IllegalStateException.class)
	public void testStartUnconfigured() {
		new AMQPEndpoint().start();
	}
	
	/**
	 * Creates an instance of {@link AMQPEndpoint} whilst
	 * mocking all RabbitMQ-Classes / ifaces involved.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAMQPLifecycle() throws IOException {
		// prepare mocks
		ConnectionFactory fac = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);
		Channel chan = mock(Channel.class);
		Queue.DeclareOk declareOK = mock(Queue.DeclareOk.class);
		
		// ConnectionFactory
		when(fac.newConnection()).thenReturn(con);
		// Connection
		when(con.createChannel()).thenReturn(chan);
		// Channel
		when(chan.queueDeclare()).thenReturn(declareOK);
		// DeclareOK result object
		String queueName = "testQueue";
		when(declareOK.getQueue()).thenReturn(queueName);
		
		AMQPEndpoint ep = new AMQPEndpoint();
		Whitebox.setInternalState(ep, "connectionFactory", fac);
		
		ep.configure(this.exampleConf);
		ep.start();
		
		// verify "important" connect methods were called
		verify(fac).newConnection();
		verify(con).createChannel();
		verify(chan).queueDeclare();
		verify(declareOK).getQueue();
		// in the example conf we're bindingn to 3 queues, let's check that
		String exchange = this.exampleConf.get(AMQPEndpoint.CONFIG_KEY_EXCHANGE_NAME);
		String routingKey = this.exampleConf.get(AMQPEndpoint.CONFIG_KEY_ROUTING_KEY);
		// listens for broadcasts
		verify(chan, times(1)).queueBind(eq(queueName), eq(exchange), eq("broadcast"));
		// listens for tasks
		verify(chan, times(1)).queueBind(eq("cit_stream_tasks_queue_" + routingKey), eq(exchange), eq("task." + routingKey));
		// listens for broadcasts to broadcast.test
		verify(chan, times(1)).queueBind(eq(queueName), eq(exchange), eq("broadcast." + routingKey));
		String exclusiveQueueName = Whitebox.getInternalState(ep, "exclusiveQueueName");
		assertEquals(queueName, exclusiveQueueName);
	}
	
	/**
	 * Tests sending a "normal" non-{@link de.tuberlin.cit.livescale.messaging.RequestMessage}
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testAMQPSendNoResponse() throws IOException, URISyntaxException {
		ConnectionFactory fac = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);
		Channel chan = mock(Channel.class);
		Queue.DeclareOk declareOK = mock(Queue.DeclareOk.class);
		
		// ConnectionFactory
		when(fac.newConnection()).thenReturn(con);
		// Connection
		when(con.createChannel()).thenReturn(chan);
		// Channel
		when(chan.queueDeclare()).thenReturn(declareOK);
		// DeclareOK result object
		String queueName = "testQueue";
		when(declareOK.getQueue()).thenReturn(queueName);
		
		AMQPEndpoint ep = new AMQPEndpoint();
		ep.setName("amqpTest");
		Whitebox.setInternalState(ep, "connectionFactory", fac);
		ep.configure(this.exampleConf);
		
		// configuredRoutingKey
		String routingKey = this.exampleConf.get(AMQPEndpoint.CONFIG_KEY_ROUTING_KEY);
		URI targetURI = new URI("amqp:///test"); 

		MessageCenter mc = new MessageCenter(false, this.emptyConf);
		mc.addEndpoint(ep);
		mc.startAllEndpoints();
		assertTrue(mc.hasEndpointToURI(targetURI));
		
		// kickin the jams
		mc.send(new TestMessage(), targetURI);
		// verify(chan, times(1)).basicPublish(eq(""), eq(routingKey), (BasicProperties) isNull(), (byte[]) any());
	}
	
	/**
	 * Tests message arrival on a {@link AMQPEndpoint} and
	 * correctness of the generated responseURI in the
	 * {@link MessageManifest} 
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testArrival() throws IOException, URISyntaxException {
		ConnectionFactory fac = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);
		Channel chan = mock(Channel.class);
		Queue.DeclareOk declareOK = mock(Queue.DeclareOk.class);
		
		// ConnectionFactory
		when(fac.newConnection()).thenReturn(con);
		// Connection
		when(con.createChannel()).thenReturn(chan);
		// Channel
		when(chan.queueDeclare()).thenReturn(declareOK);
		// DeclareOK result object
		String queueName = "testQueue";
		when(declareOK.getQueue()).thenReturn(queueName);

		AMQPEndpoint ep = new AMQPEndpoint();
		String endpointName = "amqpTest";
		ep.setName(endpointName);
		Whitebox.setInternalState(ep, "connectionFactory", fac);
		ep.configure(this.exampleConf);
		// hookup a listener
		MessageEndpointListener listener = mock(MessageEndpointListener.class);
		ep.addMessageEndpointListener(listener);
		
		// kickin the jams 
		ep.start();
		// hook up the consumer / manually call the callback
		ArgumentCaptor<DefaultConsumer> consumer = ArgumentCaptor.forClass(DefaultConsumer.class);
		// should in total consume the own queue and the tasks queue
		// depends on config settings
		verify(chan, times(2)).basicConsume(anyString(), anyBoolean(), (Consumer) anyObject());
		verify(chan).basicConsume(eq(queueName), anyBoolean(), consumer.capture());
		String replyQueueName = "replyQueue";
		// faux envelope
		Envelope envelope = new Envelope(-1l, false, "daExchange", "daRoutingKey");
		// faux properties
		BasicProperties props = new BasicProperties.Builder().
				replyTo(replyQueueName).
				appId(UUID.randomUUID().toString()).
				build();
		// faux message 
		TestRequestMessage message = new TestRequestMessage();
		// faux manifest
		MessageManifest mmIn = new MessageManifest(message, new URI("amqp:///targetQueue"));
		// call the callback function
		consumer.getValue().handleDelivery("leTag", envelope, props, MessageFactory.encode(mmIn));
		ArgumentCaptor<MessageManifest> mm = ArgumentCaptor.forClass(MessageManifest.class);
		verify(listener).handleMessageReceived(mm.capture());
		assertEquals("amqp", mm.getValue().getResponseURI().getScheme());
		assertEquals(endpointName, mm.getValue().getResponseURI().getAuthority());
		assertEquals("/" + replyQueueName, mm.getValue().getResponseURI().getPath());
	}
	
	/**
	 * 
	 */
	@Test
	public void testURIParser() throws URISyntaxException {
		URI target1 = new URI("amqp://name/routingKey");
		AMQPEndpoint.URIParser parser = new AMQPEndpoint.URIParser(target1);
		assertEquals("name", parser.getInstanceName());
		assertEquals("routingKey", parser.getRoutingKey());
		
		URI target2 = new URI("amqp://name/routingKey?exchange=daExchange");
		parser = new AMQPEndpoint.URIParser(target2);
		assertEquals("name", parser.getInstanceName());
		assertEquals("routingKey", parser.getRoutingKey());
		assertEquals("daExchange", parser.getExchangeName());
		
		URI target3 = new URI("amqp:///routingKey?exchange=daExchange");
		parser = new AMQPEndpoint.URIParser(target3);
		assertEquals("amqp", parser.getScheme());
		assertNull(parser.getInstanceName());
		assertEquals("routingKey", parser.getRoutingKey());
		assertEquals("daExchange", parser.getExchangeName());
	}
	
	/**
	 * Tests parser and 
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testURIBuilder() throws URISyntaxException {
		String routingKey = "leRoute";
		String instanceName = "leInstance";
		String exchangeName = "leExchange";
		URI uri = new AMQPEndpoint.URIBuilder().
				routingKey(routingKey).
				instanceName(instanceName).
				exchangeName(exchangeName).build();
		AMQPEndpoint.URIParser parser = new AMQPEndpoint.URIParser(new URI(uri.toString()));
		assertEquals(routingKey, parser.getRoutingKey());
		assertEquals(instanceName, parser.getInstanceName());
		assertEquals(exchangeName, parser.getExchangeName());
	}
	
	@Test
	public void testURIBuilderRoutingKeyConcat() throws URISyntaxException {
		URI uri = new AMQPEndpoint.URIBuilder().
				routingKey(new String[]{"one", "two", "three"}).
				build();
		assertEquals(uri.getPath(), "/one.two.three");
	}
}
