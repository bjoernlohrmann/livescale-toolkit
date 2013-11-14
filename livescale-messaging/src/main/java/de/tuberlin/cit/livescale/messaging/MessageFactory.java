package de.tuberlin.cit.livescale.messaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Static factory class to convert from Map<String, Object> message-maps to
 * actual Message instances
 * 
 * ATTN: this class is non-public on purpose.
 * 
 * @author Bernd Louis
 * 
 */
public class MessageFactory {
	private static final String MESSAGE_NAME = "__message_name";

	/**
	 * Creates a Message instance from a Map<String, Object>. Does some
	 * validation on the UUIDs and whether they're properly overwritten if they
	 * need to.
	 * 
	 * @param messageMap
	 * @return
	 * @throws InvalidMessageTypeException
	 *             in the corresponding message class cannot be instantiated for
	 *             some reason
	 */
	public static Message createMessage(Map<String, Object> messageMap)
			throws InvalidMessageTypeException {
		try {
			@SuppressWarnings("unchecked")
			Class<Message> messageClass = (Class<Message>) Class
					.forName(resolveClassNameFromMap(messageMap));
			Message message;
			UUID formerRequestUUID = UUID.randomUUID();
			// ReponseMessages have no nullary constructor so we gotta find
			// the UUID constructor
			if (ResponseMessage.class.isAssignableFrom(messageClass)) {
				Constructor<?> constr = messageClass.getConstructor(UUID.class);
				// UUID.randomUUID() will be overwritten with the actual thing
				// while the message class is being deserialized.
				message = (Message) constr.newInstance(formerRequestUUID);
			} else {
				message = messageClass.newInstance();
			}
			UUID formerMessageId = message.getUUID();
			// now actually initialize the message
			message.fromMap(messageMap);
			// check if the UUIDs have been properly overwritten
			if (message.getUUID().equals(formerMessageId)) {
				throw new MalformedMessageException(
						String.format(
								"%s was not properly deserialized. The UUID was not properly set after fromMap method was called. ",
								message.getClass().getSimpleName()));
			}
			// same thing here with the Request-UUID on ResponseMessage
			// instances.
			if (message instanceof ResponseMessage) {
				ResponseMessage reqMessage = (ResponseMessage) message;
				if (reqMessage.getRequestMessageUUID()
						.equals(formerRequestUUID)) {
					throw new MalformedMessageException(
							String.format(
									"%s was not properly deserialized. The requestUUID was not properly set after fromMap method was called. ",
									message.getClass().getSimpleName()));
				}
			}
			return message;
		} catch (Exception e) {
			throw new InvalidMessageTypeException(e);
		}
	}

	/**
	 * Renders a message to a serialization map
	 * 
	 * @param message
	 * @return
	 */
	public static Map<String, Object> renderMessage(Message message) {
		Map<String, Object> map = new HashMap<String, Object>();
		message.toMap(map);
		map.put(MESSAGE_NAME, message.getClass().getName());
		validateMessageUUIDRendering(message, map);
		return map;
	}
	
	/**
	 * Method renders a {@link MessageManifest} to a byte array for marshaling 
	 * 
	 * @param m
	 * @return
	 * @throws IOException
	 */
	public static byte[] encode(MessageManifest m) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutput oo = new ObjectOutputStream(baos);
		Map<String, Object> map = new HashMap<String, Object>();
		m.toMap(map);
		oo.writeObject(map);
		oo.close();
		return baos.toByteArray();
	}
	
	/**
	 * Tries to decode a byte array to a {@link MessageManifest}
	 * 
	 * @param bytes
	 * @return
	 * @throws MalformedMessageException
	 */
	public static MessageManifest decode(byte[] bytes) throws MalformedMessageException {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
			@SuppressWarnings("unchecked")
			Map<String, Object> readObject = (Map<String, Object>) ois.readObject();
			return new MessageManifest(readObject);
		} catch (IOException e) {
			throw new MalformedMessageException("Could not decode Message. Error during IO", e);
		} catch (ClassNotFoundException e) {
			throw new MalformedMessageException("Decoding expected an instance of java.util.HashMap, but got something that could not be resolved.", e);
		} catch (URISyntaxException e) {
			throw new MalformedMessageException("Message could not be decoded because of malformed URIs");
		} catch (InvalidMessageTypeException e) {
			throw new MalformedMessageException("Message could not be decoded because it's not a valid message", e);
		}
	}
	
	/**
	 * This does some validation of the provided {@link Message} and its map.
	 * 
	 * A {@link RuntimeException} will be thrown in case something's wrong.
	 * 
	 * @param message
	 * @param map
	 */
	private static void validateMessageUUIDRendering(Message message,
			Map<String, Object> messageMap) {
		if (message instanceof AbstractMessage) {
			if (!messageMap.containsKey(AbstractMessage.MESSAGE_UUID)) {
				throw new MalformedMessageException(
						String.format(
								"%s is an AbstractMessage but its serialization map does not contain a message UUID entry. Please check whether this implementation of toMap contains the super.toMap call. ",
								message.getClass().getSimpleName()));
			}
			if (message instanceof ResponseMessage) {
				if (!messageMap.containsKey(ResponseMessage.REQUEST_UUID)) {
					throw new MalformedMessageException(
							String.format(
									"%s is a ResponseMessage but its serialization map does not contain the RequestMessage-UUID. Please check whether this implementation of toMap contains the super.toMap call.",
									message.getClass().getSimpleName()));
				}
			}
		}
	}

	private static String resolveClassNameFromMap(Map<String, Object> map)
			throws IllegalArgumentException {
		if (map.containsKey(MESSAGE_NAME)) {
			return (String) map.get(MESSAGE_NAME);
		}
		throw new IllegalArgumentException(String.format(
				"Message map contains no %s element.", MESSAGE_NAME));
	}

	/**
	 * This Exception is thrown if a message could not be resolved.
	 * 
	 * Please check {@link InvalidMessageTypeException#getCause()} for details.
	 * 
	 * @author Bernd Louis
	 * 
	 */
	static class InvalidMessageTypeException extends Exception {
		private static final long serialVersionUID = -8928928132417172333L;

		public InvalidMessageTypeException(Throwable cause) {
			super(cause);
		}
	}

	/**
	 * This Exception is thrown to signal that there's an implementation error
	 * while subclassing {@link AbstractMessage} or {@link ResponseMessage}
	 * probably due to missing super-calls in the {@link Message#toMap(Map)} or
	 * {@link Message#fromMap(Map)} implementations.
	 * 
	 * @author Bernd Louis
	 * 
	 */
	static class MalformedMessageException extends RuntimeException {
		private static final long serialVersionUID = -1970895302266523698L;

		public MalformedMessageException(String s) {
			super(s);
		}
		
		public MalformedMessageException(String s, Throwable e) {
			super(s,e);
		}
	}
}
