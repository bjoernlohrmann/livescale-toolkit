package de.tuberlin.cit.livescale.messaging.messages;

import java.util.Map;
import java.util.UUID;

import de.tuberlin.cit.livescale.messaging.AbstractMessage;

/**
 * Test-Message
 * 
 * @author louis
 * 
 */
public class TestMessage extends AbstractMessage {

	private static final String FIELD_SIX = "FIELD_SIX";
	private static final String FIELD_FIVE = "FIELD_FIVE";
	private String fieldFive, fieldSix;

	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setFieldFive((String) messageMap.get(FIELD_FIVE));
		this.setFieldSix((String) messageMap.get(FIELD_SIX));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#getUUID()
	 */
	@Override
	public UUID getUUID() {
		return UUID.randomUUID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(FIELD_FIVE, this.getFieldFive());
		messageMap.put(FIELD_SIX, this.getFieldSix());
	}

	/**
	 * Returns the fieldFive.
	 * 
	 * @return the fieldFive
	 */
	public String getFieldFive() {
		return this.fieldFive;
	}

	/**
	 * Sets the fieldFive to the specified value.
	 *
	 * @param fieldFive the fieldFive to set
	 */
	public void setFieldFive(String fieldFive) {
		this.fieldFive = fieldFive;
	}

	/**
	 * Returns the fieldSix.
	 * 
	 * @return the fieldSix
	 */
	public String getFieldSix() {
		return this.fieldSix;
	}

	/**
	 * Sets the fieldSix to the specified value.
	 *
	 * @param fieldSix the fieldSix to set
	 */
	public void setFieldSix(String fieldSix) {
		this.fieldSix = fieldSix;
	}

}
