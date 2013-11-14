package de.tuberlin.cit.livescale.messaging;

import static de.tuberlin.cit.livescale.messaging.MessageFactory.createMessage;
import static de.tuberlin.cit.livescale.messaging.MessageFactory.renderMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import de.tuberlin.cit.livescale.messaging.MessageFactory.InvalidMessageTypeException;

/**
 * Represents the "shipping information" for a collection of Messages.
 * 
 * Targets and expected responses are described as URI.
 * 
 * @author louis
 * 
 */
public class MessageManifest {
	private static final String MAP_KEY_MESSAGES = "messages";
	private static final String MAP_KEY_RESPONSE_URI = "RESPONSE_URI";
	private static final String MAP_KEY_TARGET_URI = "TARGET_URI";
	
	private Collection<Message> messages = new LinkedList<Message>();
	private URI targetURI;
	private URI responseURI;
	
	/**
	 * 
	 * Initializes MessageManifest.
	 *
	 * @param map 
	 * @throws URISyntaxException
	 */
	public MessageManifest(Map<String, Object> map) throws URISyntaxException, InvalidMessageTypeException {
		this.targetURI = new URI((String) map.get(MAP_KEY_TARGET_URI));
		if(map.containsKey(MAP_KEY_RESPONSE_URI)) {
			this.responseURI = new URI((String) map.get(MAP_KEY_RESPONSE_URI));	
		}
		@SuppressWarnings("unchecked")
		Collection<Map<String, Object>> msgs = (Collection<Map<String, Object>>) map.get(MAP_KEY_MESSAGES);
		for(Map<String, Object> m : msgs) {
			this.messages.add(createMessage(m));
		}
	}
	
	/**
	 * Creates a Manifest for a single {@link Message} and target URI
	 * 
	 * @param message
	 * @param targetURI
	 */
	public MessageManifest(Message message, URI targetURI) {
		this.getMessages().add(message);
		this.setTargetURI(targetURI);
	}

	/**
	 * 
	 * Initializes MessageManifest with a {@link RequestMessage}, a targetURI
	 * and a responseURI.
	 * 
	 * A {@link RequestMessage} has to have a {@link ResponseMessage}, so a
	 * Manifest containing a <code>RequestMessage</code> needs a responseURI.
	 * 
	 * @param requestMessage
	 * @param targetURI
	 * @param responseURI
	 */
	public MessageManifest(RequestMessage requestMessage, URI targetURI,
			URI responseURI) {
		this.getMessages().add(requestMessage);
		this.setTargetURI(targetURI);
		this.setResponseURI(responseURI);
	}

	/**
	 * Creates a Manifest for a {@link Collection} of {@link Message}s and a
	 * target URI
	 * 
	 * @param messages
	 * @param targetURI
	 */
	public MessageManifest(Collection<Message> messages, URI targetURI) {
		this.setMessages(messages);
		this.setTargetURI(targetURI);
		this.setResponseURI(null);
	}

	/**
	 * Creates a Manifest for a single {@link Message} with a target and a
	 * expected response.
	 * 
	 * @param message
	 * @param targetURI
	 * @param reponseURI
	 */
	public MessageManifest(Message message, URI targetURI, URI responseURI) {
		this.getMessages().add(message);
		this.setTargetURI(targetURI);
		this.setResponseURI(responseURI);
	}

	/**
	 * does this manifest expect a response?
	 * 
	 * Returns true if the instance has a responseURI that is not null 
	 * or has {@link RequestMessage}s in the list (in that precedence order)  
	 * 
	 * @return
	 */
	public boolean isResponseRequired() {
		hasRequestMessages();
		return this.getResponseURI() != null || hasRequestMessages();
	}

	/**
	 * Returns true if any message in the list an instance of
	 * {@link MessageManifest}
	 * 
	 * @return
	 */
	private boolean hasRequestMessages() {
		for(Message m : this.messages) {
			if(m instanceof RequestMessage) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return Returns the response {@link URI} or <code>null</code> if there
	 *         isn't any
	 */
	public URI getResponseURI() {
		return this.responseURI;
	}

	/**
	 * 
	 * @param responseURI
	 *            sets the response {@link URI} or <code>null</code>
	 */
	public void setResponseURI(URI responseURI) {
		this.responseURI = responseURI;
	}

	/**
	 * 
	 * @return returns the target {@link URI}
	 */
	public URI getTargetURI() {
		return this.targetURI;
	}

	/**
	 * 
	 * @param targetURI
	 *            sets the target {@link URI}
	 */
	public void setTargetURI(URI targetURI) {
		this.targetURI = targetURI;
	}

	/**
	 * 
	 * @return all {@link Message}s in this {@link MessageManifest}
	 */
	public Collection<Message> getMessages() {
		return this.messages;
	}

	/**
	 * 
	 * @param messages
	 *            the {@link Message}s to be in this {@link MessageManifest}
	 */
	public void setMessages(Collection<Message> messages) {
		this.messages = messages;
	}
	
	public void toMap(Map<String, Object> map) {
		Collection<Map<String, Object>> messages = new LinkedList<Map<String, Object>>();
		for(Message m : this.messages) {
			messages.add(renderMessage(m));
		}
		map.put(MAP_KEY_TARGET_URI, this.targetURI.toString());
		if(this.responseURI != null) {
			map.put(MAP_KEY_RESPONSE_URI, this.responseURI.toString());
		}
		map.put(MAP_KEY_MESSAGES, messages);
	}
}
