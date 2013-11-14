package de.tuberlin.cit.livescale.messaging;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import de.tuberlin.cit.livescale.messaging.AbstractMessage;

public class AbstractMessageTest {

	@Test
	public void testRandomUUIDIsAssigned() {
		AbstractMessage am = new AbstractMessage() {

			@Override
			public void toMap(Map<String, Object> messageMap) {
				super.toMap(messageMap);
			}

			@Override
			public void fromMap(Map<String, Object> messageMap) {
				super.fromMap(messageMap);
			}
		};
		assertTrue(am.getUUID() != null);
	}

}
