/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/
package de.tuberlin.cit.livescale.messaging.endpoints;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * This class acts as a convenience superclass for {@link MessageEndpoint} classes.  
 * 
 * It handles Endpoint lifecycle events and provides handling of
 * {@link MessageEndpointListener} notifications and registrations. 
 * 
 * 
 * @author Bernd Louis
 *
 */
public abstract class AbstractMessageEndpoint implements MessageEndpoint {
	private static final Log LOG = LogFactory.getLog(AbstractMessageEndpoint.class);
	
	private Collection<MessageEndpointListener> messageEndpointListeners = new LinkedList<MessageEndpointListener>();
	private LifecycleState lifeCylceState = new InitializedState();
	private String name;
	
	@Override
	public void addMessageEndpointListener(MessageEndpointListener messageEndpointListener) {
		this.messageEndpointListeners.add(messageEndpointListener);
	}

	/**
	 * Notifies the registered {@link MessageEndpointListener}s of the current
	 * {@link MessageEndpoint} having shut down.  
	 */
	protected void notifyListenersOfShutdown() {
		for(MessageEndpointListener mel : this.messageEndpointListeners) {
			mel.handleShutdown(this);
		}
	}

	/**
	 * Notifies the registered {@link MessageEndpointListener}s of of a delivery error. 
	 */
	protected void notifyListenersDeliveryEvent(MessageDeliveryEvent messageDeliveryEvent) {
		for (MessageEndpointListener mel : this.messageEndpointListeners) {
			mel.handleDeliveryEvent(messageDeliveryEvent);
		}
	}
	
	/**
	 * Notifies all {@link MessageEndpointListener}s of an arriving
	 * {@link MessageManifest}
	 * 
	 * @param manifest
	 *            the arriving {@link MessageManifest}
	 */
	protected void notifyMessageEndpointListeners(MessageManifest manifest) {
		for (MessageEndpointListener l : this.messageEndpointListeners) {
			l.handleMessageReceived(manifest);
		}
	}
	
	@Override
	public final void start() {
		this.lifeCylceState.start(this);
	}
	
	@Override
	public final void shutdown() {
		this.lifeCylceState.shutdown(this);
	}
	
	@Override
	public final void send(MessageManifest manifest) {
		this.lifeCylceState.send(this, manifest);
	}
	
	/**
	 * This method performs the actual start of the endpoint 
	 */
	protected abstract void performStart() throws IOException;
	
	/**
	 * This method performs the actual shutdown of the endpoint.
	 */
	protected abstract void performShutdown();
	
	/**
	 * Performs the actual send of the {@link MessageManifest}
	 * 
	 * @param manifest
	 */
	protected abstract void performSend(MessageManifest manifest);
	
	/**
	 * Returns the scheme
	 * 
	 * @return
	 */
	protected abstract String getScheme();
	
	@Override
	public boolean isRunning() {
		return this.lifeCylceState instanceof RunningState;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public URI getLocalEndpointURI() {
		try {
			return new URI(getScheme(), getName(), "", "", "");
		} catch (URISyntaxException e) {
			LOG.debug(String.format("Could not create the local Endpoint URI. Scheme: %s, name: %s", getScheme(), getName()), e);
			return null;
		}
	}
	
	/**
	 * State pattern initialized/started/shutdown states of the endpoint
	 * 
	 * Is cleaner than to do it like this instead of having some
	 * combination of boolean conditions. 
	 * 
	 * @author Bernd Louis
	 */
	private interface LifecycleState {
		public void start(AbstractMessageEndpoint context);
		public void shutdown(AbstractMessageEndpoint context);
		public void send(AbstractMessageEndpoint context, MessageManifest manifest);
	}
	
	/**
	 * Represents the state "class has been created"
	 * 
	 * @author Bernd Louis
	 */
	private class InitializedState implements LifecycleState {

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#start(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void start(AbstractMessageEndpoint context) {
			try {
				context.performStart();
				context.lifeCylceState = new RunningState();
			} catch (IOException e) {
				LOG.warn("Unable to start endpoint", e);
			}
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#shutdown(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void shutdown(AbstractMessageEndpoint context) {
			throw new IllegalStateException("Cannot shutdown Endpoint, because it's not running.");
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#send(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint, de.tuberlin.cit.livescale.messaging.MessageManifest)
		 */
		@Override
		public void send(AbstractMessageEndpoint context,
				MessageManifest manifest) {
			throw new IllegalStateException("Cannot send, endpoint has not been started yet.");
		}
	}
	
	/**
	 * Represents the state "is running"
	 * 
	 * @author Bernd Louis
	 */
	private class RunningState implements LifecycleState {

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#start(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void start(AbstractMessageEndpoint context) {
			throw new IllegalStateException("Cannot start Endpoint, because it's already running");
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#shutdown(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void shutdown(AbstractMessageEndpoint context) {
			context.performShutdown();
			context.notifyListenersOfShutdown();
			context.lifeCylceState = new ShutdownState();
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#send(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint, de.tuberlin.cit.livescale.messaging.MessageManifest)
		 */
		@Override
		public void send(AbstractMessageEndpoint context,
				MessageManifest manifest) {
			context.performSend(manifest);
		}
		
	}
	/**
	 * Represents the state "has been shut down". As of now we can never leave this state. 
	 * 
	 * @author Bernd Louis
	 */
	private class ShutdownState implements LifecycleState {

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#start(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void start(AbstractMessageEndpoint context) {
			throw new IllegalStateException("Cannot start the endpoint because it's already been shut down.");
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#shutdown(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint)
		 */
		@Override
		public void shutdown(AbstractMessageEndpoint context) {
			throw new IllegalStateException("Cannot shut down the endpoint because it's already been shut down.");
		}

		/* (non-Javadoc)
		 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint.LifecycleState#send(de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint, de.tuberlin.cit.livescale.messaging.MessageManifest)
		 */
		@Override
		public void send(AbstractMessageEndpoint context,
				MessageManifest manifest) {
			throw new IllegalStateException("Cannot send on this endpoint, it has already been shut down.");
		}
		
	}
	
}