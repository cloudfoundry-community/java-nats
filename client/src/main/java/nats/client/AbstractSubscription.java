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

import nats.HandlerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an abstract implementation of the {@link Subscription} interface.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractSubscription implements Subscription {
	private final AtomicInteger receivedMessages = new AtomicInteger();
	private final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
	private final List<BlockingQueueSubscriptionIterator> iterators = new ArrayList<BlockingQueueSubscriptionIterator>();

	private final String subject;
	private final String queueGroup;
	private final Integer maxMessages;

	private final ScheduledExecutorService scheduledExecutorService;
	private final ExceptionHandler exceptionHandler;

	protected AbstractSubscription(String subject, String queueGroup, Integer maxMessages, ScheduledExecutorService scheduledExecutorService, ExceptionHandler exceptionHandler) {
		this.subject = subject;
		this.queueGroup = queueGroup;
		this.maxMessages = maxMessages;
		this.scheduledExecutorService = scheduledExecutorService;
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void close() {
		synchronized (iterators) {
			for (BlockingQueueSubscriptionIterator iterator : iterators) {
				iterator.close();
			}
		}
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public HandlerRegistration addMessageHandler(final MessageHandler messageHandler) {
		synchronized (handlers) {
			handlers.add(messageHandler);
		}
		return new HandlerRegistration() {
			@Override
			public void remove() {
				synchronized (handlers) {
					handlers.remove(messageHandler);
				}
			}
		};
	}

	@Override
	public SubscriptionIterator iterator() {
		final BlockingQueueSubscriptionIterator iterator = new BlockingQueueSubscriptionIterator();
		synchronized (iterators) {
			iterators.add(iterator);
		}
		return iterator;
	}

	@Override
	public int getReceivedMessages() {
		return receivedMessages.get();
	}

	@Override
	public Integer getMaxMessages() {
		return maxMessages;
	}

	@Override
	public String getQueueGroup() {
		return queueGroup;
	}

	@Override
	public SubscriptionTimeout timeout(long time, TimeUnit unit) {
		return new DefaultSubscriptionTimeout(scheduledExecutorService, this, exceptionHandler, time, unit);
	}

	@SuppressWarnings("ConstantConditions")
	public void onMessage(String subject, String body, String replyTo) {
		final int messageCount = receivedMessages.incrementAndGet();
		if (maxMessages != null && messageCount >= maxMessages) {
			close();
		}
		Message message = createMessage(subject, body, replyTo);
		synchronized (handlers) {
			for (MessageHandler handler : handlers) {
				try {
					handler.onMessage(message);
				} catch (Throwable t) {
					exceptionHandler.onException(t);
				}
			}
		}
		synchronized (iterators) {
			for (BlockingQueueSubscriptionIterator iterator : iterators) {
				try {
					iterator.push(message);
				} catch (Throwable t) {
					exceptionHandler.onException(t);
				}
			}
		}
	}

	protected abstract Message createMessage(String subject, String body, String replyTo);
}
