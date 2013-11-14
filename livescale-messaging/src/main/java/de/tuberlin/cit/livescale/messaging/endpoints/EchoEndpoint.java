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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import de.tuberlin.cit.livescale.messaging.MessageManifest;

/**
 * This is the default implementation to {@link MessageEndpoint}. All
 * {@link EchoEndpoint}s are linked through their static scope and may subscribe
 * to any number of Message Queues as represented as the <code>queues</code>
 * map.
 * 
 * @author louis
 * 
 */
public class EchoEndpoint extends AbstractMessageEndpoint {

	/**
	 * Map queueName => Queue of {@link MessageManifest}
	 */
	private static ConcurrentMap<String, Queue<MessageManifest>> queues = new ConcurrentHashMap<String, Queue<MessageManifest>>();

	/**
	 * queueName => queueName of EchoQueueListener
	 */
	private static ConcurrentMap<String, Collection<EchoQueueListener>> queueListeners = new ConcurrentHashMap<String, Collection<EchoQueueListener>>();

	/**
	 * This {@link MessageEndpoint}s URI-scheme.
	 */
	private static final String SCHEME_NAME_ECHO = "echo";

	/**
	 * can bet set through configuration key "observeQueue"
	 */
	private String observeQueue;

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performSend(de.tuberlin.cit.livescale.messaging.MessageManifest)
	 */
	@Override
	protected void performSend(MessageManifest manifest) {
		String queueName = manifest.getTargetURI().getPath();
		queues.putIfAbsent(queueName,
				new ConcurrentLinkedQueue<MessageManifest>());
		queues.get(queueName).add(manifest);
		notifyEchoQueueListeners(queueName);
	}
	
	/**
	 * aux method notifies all <code>queueName</code>s listeners of changes in
	 * the respective queue.
	 * 
	 * @param queueName
	 */
	private static void notifyEchoQueueListeners(String queueName) {
		Queue<MessageManifest> q = queues.get(queueName);
		while (q != null && !q.isEmpty()) {
			MessageManifest current = q.poll();
			for (EchoQueueListener listener : queueListeners.get(queueName)) {
				listener.handleQueueEvent(current);
			}
		}
	}

	/**
	 * @param callback
	 */
	private void addEchoQueueListener(final EchoQueueListener callback) {
		queueListeners.putIfAbsent(this.observeQueue,
				new LinkedList<EchoEndpoint.EchoQueueListener>());
		queueListeners.get(this.observeQueue).add(callback);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tuberlin.cit.livescale.messaging.endpoints.MessageEndpoint#configure
	 * (java.util.Map)
	 */
	@Override
	public void configure(Map<String, String> properties) {
		this.observeQueue = properties.get("observeQueue");
		this.addEchoQueueListener(new EchoQueueListener() {
			@Override
			public void handleQueueEvent(MessageManifest manifest) {
				EchoEndpoint.this.notifyMessageEndpointListeners(manifest);
			}
		});
	}

	/**
	 * Internal interface to subscribe to
	 * 
	 * @author louis
	 * 
	 */
	private static interface EchoQueueListener {
		public void handleQueueEvent(MessageManifest manifest);
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performStart()
	 */
	@Override
	protected void performStart() {
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#performShutdown()
	 */
	@Override
	protected void performShutdown() {
	}

	/* (non-Javadoc)
	 * @see de.tuberlin.cit.livescale.messaging.endpoints.AbstractMessageEndpoint#getScheme()
	 */
	@Override
	protected String getScheme() {
		return SCHEME_NAME_ECHO;
	}

}
