package de.tuberlin.cit.livescale.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Abstract superclass for messages implementing Message.
 * 
 * This provides some convenience methods as well as encapsulation of the
 * {@link UUID} uuid Attribute.
 * 
 * @author louis
 */
public abstract class AbstractMessage implements Message {
	protected static final String MESSAGE_UUID = "__uuid";
	private UUID uuid;

	/**
	 * Creates an {@link AbstractMessage} with a random UUID
	 */
	public AbstractMessage() {
		this.setUUID(UUID.randomUUID());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.Message#fromMap(java.util.Map)
	 */
	@Override
	public void fromMap(Map<String, Object> messageMap) {
		this.uuid = UUID.fromString((String) messageMap.get(MESSAGE_UUID));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.tuberlin.cit.livescale.messaging.Message#toMap(java.util.Map)
	 */
	@Override
	public void toMap(Map<String, Object> messageMap) {
		messageMap.put(MESSAGE_UUID, this.uuid.toString());
	}

	/**
	 * Creates an {@link AbstractMessage} with specific UUID
	 * 
	 * @param uuid
	 */
	public AbstractMessage(UUID uuid) {
		this.setUUID(uuid);
	}

	/**
	 * returns the Message's {@link UUID}
	 * 
	 * @return
	 */
	@Override
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Sets the Message's {@link UUID}
	 * 
	 * @param uuid
	 */
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}
}
