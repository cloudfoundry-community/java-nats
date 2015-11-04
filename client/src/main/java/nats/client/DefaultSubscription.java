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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an abstract implementation of the {@link Subscription} interface.
 *
 * @author Mike Heath
 */
public class DefaultSubscription implements Subscription {

	private final AtomicInteger receivedMessages = new AtomicInteger();
	private final List<MessageHandler> handlers = new ArrayList<>();
	private final List<BlockingQueueMessageIterator> iterators = new ArrayList<>();

	private final String subject;
	private final String queueGroup;
	private final Integer maxMessages;

	protected DefaultSubscription(String subject, String queueGroup, Integer maxMessages, MessageHandler... messageHandlers) {
		this.subject = subject;
		this.queueGroup = queueGroup;
		this.maxMessages = maxMessages;
		Collections.addAll(handlers, messageHandlers);
	}

	@Override
	public void close() {
		synchronized (iterators) {
			for (BlockingQueueMessageIterator iterator : iterators) {
				iterator.close();
			}
		}
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public Registration addMessageHandler(final MessageHandler messageHandler) {
		synchronized (handlers) {
			handlers.add(messageHandler);
		}
		return new Registration() {
			@Override
			public void remove() {
				synchronized (handlers) {
					handlers.remove(messageHandler);
				}
			}
		};
	}

	@Override
	public MessageIterator iterator() {
		final BlockingQueueMessageIterator iterator = new BlockingQueueMessageIterator();
		synchronized (iterators) {
			iterators.add(iterator);
		}
		addMessageHandler(iterator);
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

	@SuppressWarnings("ConstantConditions")
	public void onMessage(String subject, String body, String replyTo, Executor executor) {
		final int messageCount = receivedMessages.incrementAndGet();
		if (maxMessages != null && messageCount >= maxMessages) {
			close();
		}
		final Message message = createMessage(subject, body, queueGroup, replyTo);
		synchronized (handlers) {
			for (final MessageHandler handler : handlers) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						handler.onMessage(message);
					}
				});
			}
		}
	}

	protected Message createMessage(String subject, String body, String queueGroup, String replyTo) {
		return new DefaultMessage(subject, body, queueGroup, replyTo != null && replyTo.trim().length() > 0);
	}
}
