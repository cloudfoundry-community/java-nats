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
package jnats.client;

import jnats.NatsFuture;
import jnats.NatsInterruptedException;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link SubscriptionIterator} that is backed by a {@link LinkedBlockingQueue}.
 * 
 * @author Mike Heath <elcapo@gmail.com>
 */
class BlockingQueueSubscriptionIterator implements SubscriptionIterator {

	private static final Message CLOSED = new Message() {
		@Override
		public Subscription getSubscription() {
			return null;
		}

		@Override
		public String getSubject() {
			return null;
		}

		@Override
		public String getBody() {
			return null;
		}

		@Override
		public String getReplyTo() {
			return null;
		}

		@Override
		public NatsFuture reply(String message) {
			return null;
		}

		@Override
		public NatsFuture reply(String message, long delay, TimeUnit unit) {
			return null;
		}
	};

	private final BlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();
	private volatile boolean closed = false;
	
	@Override
	public boolean hasNext() {
		return queue.size() > 0 || !closed;
	}

	@Override
	public Message next() {
		try {
			final Message message = queue.take();
			if (message == CLOSED) {
				throw new NatsClosedException();
			}
			return message;
		} catch (InterruptedException e) {
			throw new NatsInterruptedException(e);
		}
	}

	@Override
	public Message next(long timeout, TimeUnit unit) {
		try {
			final Message message = queue.poll(timeout, unit);
			if (message == CLOSED) {
				throw new NatsClosedException();
			}
			return message;
		} catch (InterruptedException e) {
			throw new NatsInterruptedException(e);
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("You can't remove a message after it has been sent from the Nats server. Nice try though.");
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			queue.put(CLOSED);
		} catch (InterruptedException e) {
			throw new NatsInterruptedException(e);
		}
	}

	void push(Message message) {
		try {
			queue.put(message);
		} catch (InterruptedException e) {
			throw new NatsInterruptedException(e);
		}
	}
}
