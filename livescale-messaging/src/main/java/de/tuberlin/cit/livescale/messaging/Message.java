package de.tuberlin.cit.livescale.messaging;

import java.util.Map;
import java.util.UUID;

/**
 * Describes a Message with the capabilities to initialize itself from and to
 * render itself to a Map<String, Object>
 * 
 * @author louis
 * 
 */
public interface Message {

	/**
	 * initializes the message instance with a map of key-value pairs
	 * 
	 * NOTE: If you're subclassing from {@link AbstractMessage},
	 * {@link RequestMessage} or {@link ResponseMessage}, please remember to
	 * super-call those implementations.
	 * 
	 * @param messageMap
	 */
	public void fromMap(Map<String, Object> messageMap);

	/**
	 * returns a map of message attributes ready for marshaling
	 * 
	 * NOTE: If you're subclassing from {@link AbstractMessage},
	 * {@link RequestMessage} or {@link ResponseMessage}, please remember to
	 * super-call those implementations.
	 * 
	 * @param messageMap
	 */
	public void toMap(Map<String, Object> messageMap);

	/**
	 * returns the messages UUID
	 * 
	 * @return
	 */
	public UUID getUUID();
}
