/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
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

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Provide a mock instance of {@link Nats} to use primarily for testing purposes. This mock Nats does not support
 * wild card subjects.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockNats implements Nats {

	private volatile boolean connected = true;
	private final Map<String, Collection<MockSubscription>> subscriptions = new HashMap<String, Collection<MockSubscription>>();

	private final ExceptionHandler exceptionHandler;

	private final ConnectionStatus connectionStatus = new ConnectionStatus() {
		@Override
		public boolean isConnected() {
			return connected;
		}

		@Override
		public boolean isServerReady() {
			return true;
		}

		@Override
		public boolean awaitConnectionClose(long time, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public boolean awaitServerReady(long time, TimeUnit unit) throws InterruptedException {
			return false;
		}

		@Override
		public SocketAddress getLocalAddress() {
			return new InetSocketAddress("localhost", 12345);
		}

		@Override
		public SocketAddress getRemoteAddress() {
			return new InetSocketAddress("localhost", 4222);
		}
	};

	@Override
	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	@Override
	public void close() {
		connected = false;
	}

	@Override
	public Publication publish(String subject) {
		return publish(subject, null);
	}

	@Override
	public Publication publish(String subject, String message) {
		return publish(subject, message, null);
	}

	@Override
	public Publication publish(String subject, String message, String replyTo) {
		final DefaultPublication publication = new DefaultPublication(subject, message, replyTo, exceptionHandler);
		publication.setDone(null);
		return publication;
	}

	@Override
	public Subscription subscribe(String subject) {
		return subscribe(subject, null, null);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup) {
		return subscribe(subject, queueGroup, null);
	}

	@Override
	public Subscription subscribe(String subject, Integer maxMessages) {
		return subscribe(subject, null, maxMessages);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup, Integer maxMessages) {
		return null;
	}

	@Override
	public Request request(String subject, String message, MessageHandler... messageHandlers) {
		return null;
	}

	@Override
	public Request request(String subject, MessageHandler... messageHandlers) {
		return null;
	}

	@Override
	public Request request(String subject, String message, Integer maxReplies, MessageHandler... messageHandlers) {
		return null;
	}

	private class MockSubscription {
	}
}
