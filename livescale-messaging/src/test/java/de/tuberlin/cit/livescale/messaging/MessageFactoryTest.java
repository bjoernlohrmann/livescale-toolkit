package de.tuberlin.cit.livescale.messaging;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.tuberlin.cit.livescale.messaging.Message;
import de.tuberlin.cit.livescale.messaging.MessageFactory;
import de.tuberlin.cit.livescale.messaging.MessageFactory.InvalidMessageTypeException;
import de.tuberlin.cit.livescale.messaging.messages.TestMessage;

public class MessageFactoryTest {

	@Test
	public void testInstantiationSuccess() throws InvalidMessageTypeException {
		Message testMessage = new TestMessage();
		Message msg = MessageFactory.createMessage(MessageFactory
				.renderMessage(testMessage));
		assertTrue(msg instanceof TestMessage);
	}

	@Test(expected = InvalidMessageTypeException.class)
	public void testInstantiationFailure() throws InvalidMessageTypeException {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("drugs ain't", "cool, kids!");
		MessageFactory.createMessage(map);
	}
}
