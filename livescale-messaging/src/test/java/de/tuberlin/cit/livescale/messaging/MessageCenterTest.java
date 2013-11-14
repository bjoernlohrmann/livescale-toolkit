package de.tuberlin.cit.livescale.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import de.tuberlin.cit.livescale.messaging.Message;
import de.tuberlin.cit.livescale.messaging.MessageCenter;
import de.tuberlin.cit.livescale.messaging.MessageListener;
import de.tuberlin.cit.livescale.messaging.MessageManifest;
import de.tuberlin.cit.livescale.messaging.RequestMessage;
import de.tuberlin.cit.livescale.messaging.ResponseMessage;
import de.tuberlin.cit.livescale.messaging.endpoints.EchoEndpoint;
import de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint;
import de.tuberlin.cit.livescale.messaging.messages.TestMessage;
import de.tuberlin.cit.livescale.messaging.messages.TestRequestMessage;
import de.tuberlin.cit.livescale.messaging.messages.TestResponseMessage;

/**
 * Tests features of the {@link MessageCenter} class.
 * 
 * Please note: Some classes cannot properly be mocked because some classes have
 * to be physically present because of the Message instantiation and
 * subscription mechanisms.
 * 
 * Please check the test implementation classes
 * <ul>
 * <li>{@link TestMessage}</li>
 * <li>{@link TestRequestMessage}</li>
 * <li>{@link TestResponseMessage}</li>
 * </ul>
 * 
 * @author Bernd Louis
 * 
 */
public class MessageCenterTest {
	/**
	 * holds two sample configurations to be used with
	 * {@link MessageCenter#MessageCenter(Properties)}.
	 */
	private Properties echoProperties1, echoProperties2, emptyProperties;

	@Before
	public void setUp() {
		final String echoEndpointName = EchoEndpoint.class.getName();
		this.echoProperties1 = new Properties();
		this.echoProperties1.setProperty("noConcurrency", String.valueOf(true));
		this.echoProperties1.put("myEcho1.class", echoEndpointName);
		this.echoProperties1.put("myEcho1.observeQueue", "/queue1");
		this.echoProperties2 = new Properties();
		this.echoProperties2.setProperty("noConcurrency", String.valueOf(true));
		this.echoProperties2.put("myEcho2.class", echoEndpointName);
		this.echoProperties2.put("myEcho2.observeQueue", "/queue2");
		this.emptyProperties = new Properties();
		this.emptyProperties.setProperty("noConcurrency", String.valueOf(true));
	}

	/**
	 * Tests the inferred instantiation feature, i.e. whether the MessageCenter
	 * is able find its configuration in the class path.
	 * 
	 * A default configuration is provided in the test's resources.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	@Test
	public void testInferedInstantiation() throws IOException {
		new MessageCenter(false);
	}
	
	@Test
	public void testShortEndpointURIBehavior() throws URISyntaxException {
		MessageCenter mc = new MessageCenter(false, this.echoProperties1);
		URI baitURI = new URI("echo", "", "", "", "");
		assertTrue(mc.hasEndpointToURI(baitURI));
	}
	
	/**
	 * Tries to send a vanilla {@link Message} and checks if it reaches a mocked
	 * endpoint.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testSendMessage() throws IOException, URISyntaxException {
		MessageCenter mc = new MessageCenter(false);
		MessageEndpoint te = mock(MessageEndpoint.class);
		final String testAuthority = "testauthority";
		final String testScheme = "testscheme";
		when(te.getLocalEndpointURI()).thenReturn(
				new URI(testScheme, testAuthority, null, null, null)
		);
		Whitebox.setInternalState(mc, "messageEndpoints",
				Arrays.asList(new MessageEndpoint[] { te }));
		Message message = mock(Message.class);
		URI target = new URI(testScheme, testAuthority, "/testqueue", "");
		assertTrue(
				"Message Center should be configured with an endpoint for TestEndpoint",
				mc.hasEndpointToURI(target));
		mc.send(message, target);

		verify(te, times(1)).send(any(MessageManifest.class));
	}

	/**
	 * Configures a {@link MessageCenter} with the {@link EchoEndpoint} and
	 * checks if the configuration is passed properly.
	 */
	@Test
	public void testEchoEndpointInitialization() {
		String testAuthority = "myEcho1";
		Properties prop = new Properties();
		prop.put("myEcho1.class", EchoEndpoint.class.getName());
		prop.put("myEcho1.authority", testAuthority);
		prop.put("myEcho1.observeQueue", "queue1");
		MessageCenter mc1 = new MessageCenter(false, prop);
		// get the instance
		Collection<MessageEndpoint> ep = Whitebox.getInternalState(mc1,
				"messageEndpoints");
		MessageEndpoint firstEndpoint = ep.iterator().next();
		assertEquals(
				"Endpoint is supposed to be initialized with the right options",
				testAuthority, firstEndpoint.getLocalEndpointURI().getAuthority());
	}

	/**
	 * Tests the delivery of a simple {@link Message}.
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testMessageDelivery() throws URISyntaxException {
		MessageCenter mc1 = new MessageCenter(true, this.echoProperties1);
		MessageCenter mc2 = new MessageCenter(true, this.echoProperties2);
		MessageListener<TestMessage> listener = mock(MessageListener.class);
		mc2.addMessageListener(TestMessage.class, listener);
		TestMessage msg = new TestMessage();
		URI target = new URI("echo", "myEcho1", "/queue2", "", "");
		mc1.send(msg, target);
		verify(listener, times(1)).handleMessageReceived(msg);
	}

	/**
	 * Checks the Request-Reponse Mechanism between two {@link MessageCenter}
	 * instances.
	 * 
	 * The {@link EchoEndpoint} is used to check this.
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testRequestResponseMessage() throws URISyntaxException {
		final RequestMessage requestMessage = new TestRequestMessage();
		final MessageCenter mc1 = new MessageCenter(true, this.echoProperties1);
		MessageListener<TestResponseMessage> mockMessageListener1 = mock(MessageListener.class);
		mc1.addMessageListener(TestResponseMessage.class, mockMessageListener1);
		mc1.addMessageListener(TestResponseMessage.class,
				new MessageListener<TestResponseMessage>() {
					@Override
					public void handleMessageReceived(TestResponseMessage message) {
						assertTrue(message instanceof ResponseMessage);
					}
				});
		final MessageCenter mc2 = new MessageCenter(true, this.echoProperties2);
		MessageListener<TestRequestMessage> mockMessageListener2 = mock(MessageListener.class);
		mc2.addMessageListener(TestRequestMessage.class, mockMessageListener2);
		mc2.addMessageListener(TestRequestMessage.class, 
				new MessageListener<TestRequestMessage>() {
					@Override
					public void handleMessageReceived(TestRequestMessage message) {
						final RequestMessage reqM = message;
						final ResponseMessage respM = reqM.getResponseMessage();
						mc2.sendResponse(respM);
					}
		});

		URI targetURI = new URI("echo", "myEcho1", "/queue2", "", "");
		URI responseURI = new URI("echo", "myEcho2", "/queue1", "", "");
		mc1.send(requestMessage, targetURI, responseURI);
		verify(mockMessageListener2, times(1)).handleMessageReceived(
				any(TestRequestMessage.class));
		verify(mockMessageListener1, times(1)).handleMessageReceived(
				any(TestResponseMessage.class));
	}
	
	@Test
	public void testAutostartBehavior() {
		MessageCenter mc = new MessageCenter(true, this.emptyProperties);
		MessageEndpoint me = mock(MessageEndpoint.class);
		mc.addEndpoint(me);
		verify(me, times(1)).start();
	}
	
	@Test
	public void testNoAutostartBehavior() {
		MessageCenter mc = new MessageCenter(false, this.emptyProperties);
		MessageEndpoint me = mock(MessageEndpoint.class);
		mc.addEndpoint(me);
		verify(me, times(0)).start();
	}
	
	@Test
	public void testStartAnEndpointWithInferredConfig() throws IOException {
		MessageCenter mc = new MessageCenter(false);
		// myEcho
		String endpointName = "myEcho";
		mc.startEndpoint(endpointName);
		mc.stopEndpoint(endpointName);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testStartNonexistingEndpointFail() throws IOException {
		MessageCenter mc = new MessageCenter(false);
		mc.startEndpoint("wurstendpoint");
	}
	
	@Test(expected=IllegalStateException.class)
	public void testDoubleStartEndpointFail() throws IOException {
		MessageCenter mc = new MessageCenter(true);
		mc.startEndpoint("myEcho");
	}
}
