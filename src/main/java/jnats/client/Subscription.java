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

import java.io.Closeable;

/**
 * Represents a Nats subscription.
 * 
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Subscription extends Closeable, Iterable<Message> {

	/**
	 * Unsubscribes the current subscription and closes any {@link SubscriptionIterator}s associated with this
	 * subscription.
	 */
	@Override
	void close();

	/**
	 * Returns this subscription's Nats subject.
	 *
	 * @return this subscription's Nats subject
	 */
	String getSubject();

	/**
	 * Adds the specified message handler to this subscription.  The message handler is invoked when a message arrives
	 * for this subscription.
	 *
	 * @param messageHandler the message handler that is invoked when a message arrives
	 * @return a handler registration
	 */

	HandlerRegistration addMessageHandler(MessageHandler messageHandler);

	/**
	 * Returns the number of messages this handler has received.
	 *
	 * @return the number of messages this handler has received.
	 */
	int getReceivedMessages();

	/**
	 * Returns the maximum number of messages this subscription will received before closing or {@code null} if there
	 * is no limit specified.
	 *
	 * @return the maximum number of messages this subscription will received before closing or {@code null} if there
	 *         is no limit specified
	 */
	Integer getMaxMessages();

	/**
	 * Returns the queue group the subscription is participating in.
	 *
	 * @return the queue group the subscription is participating in
	 */
	String getQueueGroup();

	/**
	 * Creates a {@link SubscriptionIterator} that can be used for fetching messages from this subscription in a
	 * blocking manner. Because {@code Subscription} implements the {@link Iterable} interface, a subscription can be
	 * used in a Java for loop. For example:
	 *
	 * <pre>
	 *     for (Message message : nats.subscribe("foo.>") {
	 *         System.out.println(message);
	 *     }
	 * </pre>
	 *
	 * The for loop may terminate with an exception when the subscription is closed.
	 *
	 * @return a {@link SubscriptionIterator}
	 */
	@Override
	SubscriptionIterator iterator();
}
