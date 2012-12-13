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
import nats.NatsException;
import nats.codec.ClientUnsubscribeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractSubscription implements Subscription {
	private final AtomicInteger receivedMessages = new AtomicInteger();
	private final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
	private final List<BlockingQueueSubscriptionIterator> iterators = new ArrayList<BlockingQueueSubscriptionIterator>();

	@Override
	public void close() {
		synchronized (iterators) {
			for (BlockingQueueSubscriptionIterator iterator : iterators) {
				iterator.close();
			}
		}
		synchronized (subscriptions) {
			subscriptions.remove(id);
		}
		if (maxMessages == null) {
			if (!closed) {
				channel.write(new ClientUnsubscribeMessage(id, maxMessages));
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
	@SuppressWarnings("ConstantConditions")
	public void onMessage(final String subject, final String body, final String replyTo) {
		final int messageCount = receivedMessages.incrementAndGet();
		if (maxMessages != null && messageCount >= maxMessages) {
			close();
		}
		final Subscription subscription = this;
		final boolean hasReply = replyTo != null && replyTo.trim().length() > 0;
		Message message = new Message() {
			@Override
			public Subscription getSubscription() {
				return subscription;
			}

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public String getBody() {
				return body;
			}

			@Override
			public String getReplyTo() {
				return replyTo;
			}

			@Override
			public Publication reply(String message) {
				if (!hasReply) {
					throw new NatsException("Message does not have a replyTo address to send the message to.");
				}
				return publish(replyTo, message);

			}

			@Override
			public Publication reply(final String message, long delay, TimeUnit unit) {
				if (!hasReply) {
					throw new NatsException("Message does not have a replyTo address to send the message to.");
				}
				final DefaultPublication publication = new DefaultPublication(replyTo, message, null, exceptionHandler);
				scheduledExecutorService.schedule(new ScheduledPublication() {
					@Override
					public void run() {
						publish(publication);
					}

					@Override
					public DefaultPublication getPublication() {
						return publication;
					}
				}, delay, unit);
				return publication;
			}

			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append("[subject: '").append(subject).append("', body: '").append(body).append("'");
				if (hasReply) {
					builder.append(", replyTo: '").append(replyTo).append("'");
				}
				builder.append(']');
				return builder.toString();
			}
		};
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

	@Override
	public SubscriptionTimeout timeout(long time, TimeUnit unit) {
		return new DefaultSubscriptionTimeout(scheduledExecutorService, this, exceptionHandler, time, unit);
	}

	@Override
	public String getId() {
		return id;
	}
}
