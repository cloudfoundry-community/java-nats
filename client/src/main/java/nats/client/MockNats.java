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

import nats.NatsException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Provide a mock instance of {@link Nats} to use primarily for testing purposes. This mock Nats does not yet support
 * subscribing to subjects with wild cards.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockNats implements Nats {

	private volatile boolean connected = true;
	private final Map<String, Collection<DefaultSubscription>> subscriptions = new HashMap<String, Collection<DefaultSubscription>>();

	private final ExceptionHandler exceptionHandler = new ExceptionHandler() {
		@Override
		public void onException(Throwable t) {
			t.printStackTrace();
		}
	};

	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

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
		publish(publication);
		return publication;
	}

	private void publish(DefaultPublication publication) {
		final Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(publication.getSubject());
		if (mockSubscriptions != null) {
			for (DefaultSubscription subscription : mockSubscriptions) {
				subscription.onMessage(publication.getSubject(), publication.getMessage(), publication.getReplyTo());
			}
		}
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
		final DefaultSubscription subscription = new DefaultSubscription(subject, queueGroup, maxMessages, scheduledExecutorService, exceptionHandler) {
			@Override
			protected Message createMessage(final String subject, String body, String replyTo) {
				final boolean hasReply = replyTo != null && replyTo.trim().length() > 0;
				return new DefaultMessage(this, subject, body, replyTo) {
					@Override
					public Publication reply(String message) {
						if (!hasReply) {
							throw new NatsException("Message does not have a replyTo address to send the message to.");
						}
						return publish(subject, message);
					}

					@Override
					public Publication reply(String message, long delay, TimeUnit unit) {
						if (!hasReply) {
							throw new NatsException("Message does not have a replyTo address to send the message to.");
						}
						final DefaultPublication publication = new DefaultPublication(getReplyTo(), message, null, exceptionHandler);
						scheduledExecutorService.schedule(new Runnable() {
							@Override
							public void run() {
								publish(publication);
							}
						}, delay, unit);
						return publication;
					}
				};
			}
		};
		Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(subject);
		if (mockSubscriptions == null) {
			mockSubscriptions = new ArrayList<DefaultSubscription>();
			subscriptions.put(subject, mockSubscriptions);
		}
		mockSubscriptions.add(subscription);
		return subscription;
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

}
