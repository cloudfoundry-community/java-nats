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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an abstract implementation of the {@link Subscription} interface.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class DefaultSubscription implements Subscription {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscription.class);

	private final AtomicInteger receivedMessages = new AtomicInteger();
	private final List<MessageHandler> handlers = new ArrayList<>();
	private final List<BlockingQueueMessageIterator> iterators = new ArrayList<>();

	private final String subject;
	private final String queueGroup;
	private final Integer maxMessages;

	protected DefaultSubscription(String subject, String queueGroup, Integer maxMessages) {
		this.subject = subject;
		this.queueGroup = queueGroup;
		this.maxMessages = maxMessages;
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
	public MessageIterator iterator() {
		final BlockingQueueMessageIterator iterator = new BlockingQueueMessageIterator();
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

	@SuppressWarnings("ConstantConditions")
	public void onMessage(String subject, String body, String replyTo) {
		final int messageCount = receivedMessages.incrementAndGet();
		if (maxMessages != null && messageCount >= maxMessages) {
			close();
		}
		Message message = createMessage(subject, body, queueGroup, replyTo);
		synchronized (handlers) {
			for (MessageHandler handler : handlers) {
				try {
					handler.onMessage(message);
				} catch (Throwable t) {
					LOGGER.error("Error handling message", t);
				}
			}
		}
	}

	protected Message createMessage(String subject, String body, String queueGroup, String replyTo) {
		return new DefaultMessage(subject, body, queueGroup, replyTo != null);
	}
}
