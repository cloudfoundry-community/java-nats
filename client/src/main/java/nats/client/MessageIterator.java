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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Provides an {@link Iterator} for fetching Nats subscription messages in a blocking manner.
 *
 * @author Mike Heath
 */
public interface MessageIterator extends Iterator<Message>, AutoCloseable {

	/**
	 * Returns {@code true} if this iterator is open. We do not know if/when the Nats server will publish another
	 * message so we assume another message is pending so, this method always returns true unless this iterator has
	 * been closed.
	 *
	 * <p>Closing the subscription associated with this iterator will close this iterator.
	 *
	 * @return {@code true} unless this iterator has been closed.
	 */
	@Override
	boolean hasNext();

	/**
	 * Returns the next {@link Message} sent by the Nats server.
	 *
	 * @return the next {@link Message} sent by the Nats server.
	 * @throws NatsClosedException thrown if the iterator closes while waiting for a message.
	 */
	@Override
	Message next() throws NatsClosedException;

	/**
	 * Returns the next {@link Message} sent by the Nats server within the specified time limit.
	 *
	 * @param timeout the maximum time to wait for a message
	 * @param unit    the time unit of the {@code timeout} argument
	 * @return the next {@link Message} sent by the Nats server or {@code null} if a message didn't arrive within the
	 *         specified time limit.
	 * @throws NatsClosedException thrown if the iterator closes while waiting for a message.
	 */
	Message next(long timeout, TimeUnit unit) throws NatsClosedException;

	/**
	 * Throws an {@link UnsupportedOperationException} since you obviously can't remove a message after it has been
	 * received.
	 */
	@Override
	void remove();

	/**
	 * Closes this subscription iterator.
	 *
	 * <p>Closing the subscription associated with this iterator will close this iterator.
	 */
	@Override
	void close();
}
