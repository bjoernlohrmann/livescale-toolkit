package de.tuberlin.cit.livescale.messaging.messages;

import java.util.Map;

import de.tuberlin.cit.livescale.messaging.RequestMessage;
import de.tuberlin.cit.livescale.messaging.ResponseMessage;

/**
 * Client Message to Request a stream endpoint from the dispatcher. This is a
 * dispatcher broadcast.
 * 
 * @author Bernd Louis
 */
public class DispatcherRequestStreamRcv extends RequestMessage {
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";
	/**
	 * The stream endpoint token
	 */
	private String token;

	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setToken((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
	}

	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN, this.getToken());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.RequestMessage#getResponseMessage
	 * ()
	 */
	@Override
	public ResponseMessage getResponseMessage() {
		return new ClientStreamRcv(this.getUUID());
	}

	/**
	 * Returns the token.
	 * 
	 * @return the token
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * Sets the token to the specified value.
	 * 
	 * @param token
	 *            the token to set
	 */
	public void setToken(String token) {
		if (token == null) {
			throw new NullPointerException("token must not be null");
		}

		this.token = token;
	}

}
