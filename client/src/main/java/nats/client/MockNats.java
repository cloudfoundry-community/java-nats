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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide a mock instance of {@link Nats} to use primarily for testing purposes. This mock Nats does not yet support
 * subscribing to subjects with wild cards.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockNats implements Nats {

	private volatile boolean connected = true;
	private final Map<String, Collection<DefaultSubscription>> subscriptions = new HashMap<>();
	private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

	private final Executor executor = new Executor() {
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


	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isClosed() {
		return !connected;
	}

	@Override
	public void close() {
		connected = false;
		scheduledExecutorService.shutdown();
	}

	@Override
	public void publish(String subject) {
		publish(subject, null);
	}

	@Override
	public void publish(String subject, String body) {
		publish(subject, body, null);
	}

	@Override
	public void publish(String subject, String body, String replyTo) {
		final Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(subject);
		if (mockSubscriptions != null) {
			for (DefaultSubscription subscription : mockSubscriptions) {
				subscription.onMessage(subject, body, replyTo, executor);
			}
		}
	}

	@Override
	public Registration publish(String subject, long period, TimeUnit unit) {
		return publish(subject, "", null, period, unit);
	}

	@Override
	public Registration publish(String subject, String body, long period, TimeUnit unit) {
		return publish(subject, body, null, period, unit);
	}

	@Override
	public Registration publish(final String subject, final String body, final String replyTo, long period, TimeUnit unit) {
		final ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isConnected()) {
					publish(subject, body, replyTo);
				}
			}
		}, 0l, period, unit);
		return new Registration() {
			@Override
			public void remove() {
				scheduledFuture.cancel(false);
			}
		};
	}

	@Override
	public Subscription subscribe(String subject, MessageHandler... messageHandlers) {
		return subscribe(subject, null, null, messageHandlers);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup, MessageHandler... messageHandlers) {
		return subscribe(subject, queueGroup, null, messageHandlers);
	}

	@Override
	public Subscription subscribe(String subject, Integer maxMessages, MessageHandler... messageHandlers) {
		return subscribe(subject, null, maxMessages, messageHandlers);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup, Integer maxMessages, MessageHandler... messageHandlers) {
		final DefaultSubscription subscription = new DefaultSubscription(subject, queueGroup, maxMessages, messageHandlers) {
			@Override
			protected Message createMessage(String subject, String body, String queueGroup, final String replyTo) {
				return new DefaultMessage(subject, body, queueGroup, replyTo != null && replyTo.trim().length() > 0) {
					@Override
					public void reply(String body) throws UnsupportedOperationException {
						publish(replyTo, body);
					}

					@Override
					public void reply(String body, long delay, TimeUnit timeUnit) throws UnsupportedOperationException {
						publish(replyTo, body, delay, timeUnit);
					}
				};
			}
		};
		Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(subject);
		if (mockSubscriptions == null) {
			mockSubscriptions = new ArrayList<>();
			subscriptions.put(subject, mockSubscriptions);
		}
		mockSubscriptions.add(subscription);
		return subscription;
	}

	@Override
	public Request request(String subject, long timeout, TimeUnit unit, MessageHandler... messageHandlers) {
		return request(subject, "", timeout, unit, messageHandlers);
	}

	@Override
	public Request request(String subject, String message, long timeout, TimeUnit unit, MessageHandler... messageHandlers) {
		return request(subject, message, timeout, unit, null, messageHandlers);
	}

	@Override
	public Request request(final String subject, String message, long timeout, TimeUnit unit, final Integer maxReplies, MessageHandler... messageHandlers) {
		final String replySubject = UUID.randomUUID().toString();

		final Subscription subscription = subscribe(replySubject, maxReplies, messageHandlers);

		final AtomicInteger messageCounter = new AtomicInteger();
		subscription.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				final int count = messageCounter.incrementAndGet();
				if (maxReplies != null && count > maxReplies) {
					subscription.close();
				}
			}
		});

		publish(subject, message, replySubject);

		return new Request() {
			@Override
			public void close() {
				subscription.close();
			}

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public int getReceivedReplies() {
				return messageCounter.get();
			}

			@Override
			public Integer getMaxReplies() {
				return maxReplies;
			}
		};
	}

}
