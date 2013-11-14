package de.tuberlin.cit.livescale.messaging.endpoints;

import java.net.URI;
import java.util.List;
import java.util.Map;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * Represents a "physical" endpoint to a message service (for instance AMQP /
 * RabbitMQ).
 * 
 * @author Bernd Louis
 * 
 */
public interface MessageEndpoint {
	/**
	 * Send a message through this endpoint
	 * 
	 * @param manifest
	 */
	public void send(MessageManifest manifest);

	/**
	 * Add an observing instance
	 * 
	 * @param messageEndpointListener
	 */
	public void addMessageEndpointListener(
			MessageEndpointListener messageEndpointListener);

	/**
	 * Sets the configuration of the endpoint according to a Map<String, String>
	 * 
	 * @param properties
	 */
	public void configure(Map<String, String> properties);

	/**
	 * Returns a preinitialized {@link URI} that this Endpoint will answer to 
	 * 
	 * @return  the {@link URI}
	 */
	public URI getLocalEndpointURI();
	
	/**
	 * Starts this {@link MessageEndpoint} (if necessary)
	 */
	public void start();
	
	/**
	 * Shuts down this {@link MessageEndpoint} from this point the endpoint 
	 * is unusable and will not answer to send-calls and the like.  
	 */
	public void shutdown();
	
	/**
	 * Returns true if the endpoint is currently running. 
	 * 
	 * @return
	 */
	public boolean isRunning();
	
	/**
	 * Set the user specified name of this endpoint. 
	 * The name (normally) acts as the hostname in the 
	 * {@link MessageEndpoint#getLocalEndpointURI()} method, so it should
	 * only consist of characters you'd expect in a hostname (\w+).  
	 * 
	 * @param name
	 */
	public void setName(String name);
	
	/**
	 * Returns the user specified name of this endpoint
	 * 
	 * @return
	 */
	public String getName();

}
