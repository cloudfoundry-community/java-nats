/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package nats.client;

import io.netty.channel.EventLoopGroup;
import nats.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsConnector {
	List<URI> hosts = new ArrayList<>();
	boolean automaticReconnect = true;
	long reconnectWaitTime = Constants.DEFAULT_RECONNECT_TIME_WAIT;
	boolean pedantic = false;
	EventLoopGroup eventLoopGroup;
	int maxFrameSize = Constants.DEFAULT_MAX_FRAME_SIZE;
	final List<ConnectionStateListener> listeners = new ArrayList<>();
	long idleTimeout = Constants.DEFAULT_IDLE_TIMEOUT;
	long pingInterval = Constants.DEFAULT_PING_INTERVAL;

	/**
	 * Executor to use for invoking callbacks. By default the current thread, usually a Netty IO thread, is used to
	 * invoke callbacks.
	 */
	Executor callbackExecutor = new Executor() {
		private final Logger logger = LoggerFactory.getLogger(getClass());
		@Override
		public void execute(Runnable command) {
			try {
				command.run();
			} catch (Exception e) {
				logger.error("Error invoking callback", e);
			}
		}
	};

	/**
	 * Adds a URI to the list of URIs that will be used to connect to a Nats server by the {@link Nats} instance.
	 *
	 * @param uri a Nats URI referencing a Nats server.
	 * @return this connector.
	 */
	public NatsConnector addHost(URI uri) {
		if (!Constants.PROTOCOL.equalsIgnoreCase(uri.getScheme())) {
			throw new IllegalArgumentException("Invalid protocol in URL: " + uri);
		}
		hosts.add(uri);
		return this;
	}

	/**
	 * Adds a URI to the list of URIs that will be used to connect to a Nats server by the {@link Nats} instance.
	 *
	 * @param uri a Nats URI referencing a Nats server.
	 * @return this connector.
	 */
	public NatsConnector addHost(String uri) {
		return addHost(URI.create(uri));
	}

	/**
	 * Indicates whether a reconnect should be attempted automatically if the Nats server connection fails. Thsi
	 * value is {@code true} by default.
	 *
	 * @param automaticReconnect whether a reconnect should be attempted automatically if the Nats server
	 *                           connection fails.
	 * @return this connector.
	 */
	public NatsConnector automaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
		return this;
	}

	/**
	 * Specifies the Netty {@link EventLoopGroup} to use for connecting to the Nats server(s). (optional)
	 *
	 * @param eventLoopGroup the Netty {@code ChannelFactory} to use for connecting to the Nats server(s)
	 * @return this connector.
	 */
	public NatsConnector eventLoopGroup(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
		return this;
	}

	/**
	 * Specifies the time between sending ping requests to the Nats server.
	 *
	 * @param pingInterval the time between ping packets in milliseconds.
	 * @return this connector.
	 */
	public NatsConnector pingInterval(long pingInterval) {
		this.pingInterval = pingInterval;
		return this;
	}

	/**
	 *  Specifies the time duration the connection to the NATS server may be idle before the client closes the
	 *  connection.
	 */
	public NatsConnector idleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
		return this;
	}

	/**
	 * Specifies the amount of time to wait between connection attempts. This is only used when automatic
	 * reconnect is enabled.
	 *
	 * @param time the amount of time to wait between connection attempts.
	 * @param unit the time unit of the {@code time} argument
	 * @return this connector.
	 */
	public NatsConnector reconnectWaitTime(long time, TimeUnit unit) {
		this.reconnectWaitTime = unit.toMillis(time);
		return this;
	}

	/**
	 * Indicates whether the server should do extra checking, mostly around properly formed subjects.
	 *
	 * @param pedantic indicates whether the server should do extra checking, mostly around properly formed subjects
	 * @return this connector.
	 */
	public NatsConnector pedantic(boolean pedantic) {
		this.pedantic = pedantic;
		return this;
	}

	/**
	 * Specified the maximum message size that can be received by the {@code} Nats instance. Defaults to 1MB.
	 *
	 * @param maxFrameSize the maximum message size that can be received by the {@code} Nats instance.
	 * @return this connector.
	 */
	public NatsConnector maxFrameSize(int maxFrameSize) {
		this.maxFrameSize = maxFrameSize;
		return this;
	}

	/**
	 * Specifies the executor to use for invoking callbacks such as
	 * {@link ConnectionStateListener#onConnectionStateChange(Nats, nats.client.ConnectionStateListener.State)} and
	 * {@link MessageHandler#onMessage(Message)}.
	 *
	 * @param executor the executor to use for invoking callbacks.
	 * @return this connector.
	 */
	public NatsConnector calllbackExecutor(Executor executor) {
		this.callbackExecutor = executor;
		return this;
	}

	/**
	 * Adds a {@link ConnectionStateListener} to the client. This allows you to be notified when a connection is
	 * established, when the server is ready to process messages, and when the connection disconnects. If the
	 * connection to the server closes unexpectedly, the client will automatically try to reconnect to the Cloud
	 * Event Bus cluster.
	 *
	 * @param listener the listener to use
	 * @return this connector.
	 */
	public NatsConnector addConnectionStateListener(ConnectionStateListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * Creates the {@code Nats} instance and asynchronously connects to the first Nats server provided using the
	 * {@code #addHost} methods.
	 *
	 * @return the {@code Nats} instance.
	 */
	public Nats connect() {
		if (hosts.size() == 0) {
			throw new IllegalStateException("No host specified to connect to.");
		}
		return new NatsImpl(this);
	}


}
