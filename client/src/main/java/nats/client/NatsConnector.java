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

import nats.Constants;
import org.jboss.netty.channel.ChannelFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsConnector {
	List<URI> hosts = new ArrayList<>();
	boolean automaticReconnect = true;
	int maxReconnectAttempts = Constants.DEFAULT_MAX_RECONNECT_ATTEMPTS;
	long reconnectWaitTime = Constants.DEFAULT_RECONNECT_TIME_WAIT;
	boolean pedantic = false;
	ChannelFactory channelFactory;
	int maxMessageSize = Constants.DEFAULT_MAX_MESSAGE_SIZE;

	/**
	 * Adds a URI to the list of URIs that will be used to connect to a Nats server by the {@link Nats} instance.
	 *
	 * @param uri a Nats URI referencing a Nats server.
	 * @return this {@code Builder} instance.
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
	 * @return this {@code Builder} instance.
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
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector automaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
		return this;
	}

	/**
	 * Specifies the Netty {@link ChannelFactory} to use for connecting to the Nats server(s). (optional)
	 *
	 * @param channelFactory the Netty {@code ChannelFactory} to use for connecting to the Nats server(s)
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector channelFactory(ChannelFactory channelFactory) {
		this.channelFactory = channelFactory;
		return this;
	}

	/**
	 * Specifies the maximum number of subsequent connection attempts to make for a given server. (optional)
	 *
	 * @param maxReconnectAttempts the maximum number of subsequent connection attempts to make for a given server
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector maxReconnectAttempts(int maxReconnectAttempts) {
		this.maxReconnectAttempts = maxReconnectAttempts;
		return this;
	}

	/**
	 * Specifies the amount of time to wait between connection attempts. This is only used when automatic
	 * reconnect is enabled.
	 *
	 * @param time the amount of time to wait between connection attempts.
	 * @param unit the time unit of the {@code time} argument
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector reconnectWaitTime(long time, TimeUnit unit) {
		this.reconnectWaitTime = unit.toMillis(time);
		return this;
	}

	/**
	 * Indicates whether the server should do extra checking, mostly around properly formed subjects.
	 *
	 * @param pedantic
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector pedantic(boolean pedantic) {
		this.pedantic = pedantic;
		return this;
	}

	/**
	 * Specified the maximum message size that can be received by the {@code} Nats instance. Defaults to 1MB.
	 *
	 * @param maxMessageSize the maximum message size that can be received by the {@code} Nats instance.
	 * @return this {@code Builder} instance.
	 */
	public NatsConnector maxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
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
