package de.tuberlin.cit.livescale.messaging.messages;

import java.util.Map;
import java.util.UUID;

import de.tuberlin.cit.livescale.messaging.ResponseMessage;

/**
 * Response from the dispatcher to a {@link DispatcherRequestStreamRcv}
 * {@link Message} containing the
 * <ul>
 * <li>sender name</li>
 * <li>the endpoint address</li>
 * <li>the endpoint port</li>
 * <li>the endpoint token</li>
 * </ul>
 * 
 * @author Bernd Louis
 */
public class ClientStreamRcv extends ResponseMessage {

	/**
	 * Initializes ClientStreamRcv.
	 * 
	 * @param requestMessageUUID
	 */
	public ClientStreamRcv(UUID requestMessageUUID) {
		super(requestMessageUUID);
	}

	private static final String MAP_KEY_USER_NAME_SENDER = "MAP_KEY_USER_NAME_SENDER";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN = "MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_PORT = "MAP_KEY_STREAM_RCV_ENDPOINT_PORT";
	private static final String MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS = "MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS";
	private String address;
	private int port;
	private String token;
	private String userNameSender;

	@Override
	public void fromMap(Map<String, Object> messageMap) {
		super.fromMap(messageMap);
		this.setAddress((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS));
		this.setPort(Integer.parseInt((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_PORT)));
		this.setToken((String) messageMap
				.get(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN));
		this.setUserNameSender((String) messageMap
				.get(MAP_KEY_USER_NAME_SENDER));
	}

	@Override
	public void toMap(Map<String, Object> messageMap) {
		super.toMap(messageMap);
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_ADDRESS, this.getAddress());
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_PORT,
				String.valueOf(this.getPort()));
		messageMap.put(MAP_KEY_STREAM_RCV_ENDPOINT_TOKEN, this.getToken());
		messageMap.put(MAP_KEY_USER_NAME_SENDER, this.getUserNameSender());
	}

	/**
	 * @return a {@link String} containing the address of the endpoint
	 */
	public String getAddress() {
		return this.address;
	}

	/**
	 * @param address
	 *            of the endpoint
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * 
	 * @return the endpoint port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * 
	 * @param port
	 *            the endpoint port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 
	 * @return the endpoint token
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * 
	 * @param the
	 *            endpoint token
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * 
	 * @return the sender's user name
	 */
	public String getUserNameSender() {
		return this.userNameSender;
	}

	/**
	 * 
	 * @param userNameSender
	 *            the sender's user name
	 */
	public void setUserNameSender(String userNameSender) {
		this.userNameSender = userNameSender;
	}
}
