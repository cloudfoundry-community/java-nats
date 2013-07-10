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

/**
 * Represents a NATS subscription.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Subscription extends AutoCloseable, Iterable<Message> {

	/**
	 * Closes this subscription. Any {@link MessageHandler} objects associated with this request will no longer receive
	 * messages for this subscription after this method is invoked. All {@link MessageIterator} objects created by
	 * this subscription will also be closed.
	 */
	@Override
	void close();

	/**
	 * Returns this subscription's NATS subject.
	 *
	 * @return this subscription's NATS subject
	 */
	String getSubject();

	/**
	 * Returns the number of messages this subscription has received.
	 *
	 * @return the number of messages this subscription has received.
	 */
	int getReceivedMessages();

	/**
	 * Returns the maximum number of messages this subscription will receive before being closed automatically.
	 *
	 * @return the maximum number of messages this subscription will receive or {@code null} if no maximum was specified.
	 */
	Integer getMaxMessages();

	/**
	 * Returns the queue group the subscription is participating in.
	 *
	 * @return the queue group the subscription is participating in
	 */
	String getQueueGroup();

	/**
	 * Creates a {@link MessageIterator} that can be used for fetching messages from this subscription in a
	 * blocking manner. Because {@code Subscription} implements the {@link Iterable} interface, a subscription can be
	 * used in a Java for loop. For example:
	 * <p/>
	 * <pre>
	 *     for (Message message : nats.subscribe("foo.>") {
	 *         System.out.println(message);
	 *     }
	 * </pre>
	 * <p/>
	 * The for loop may terminate with an exception when the subscription is closed.
	 *
	 * @return a {@link MessageIterator}
	 */
	@Override
	MessageIterator iterator();

	/**
	 * Registers a {@link MessageHandler} instance with this subscription that will be invoked every time the
	 * subscription receives a message.
	 *
	 * @param messageHandler the message handler that is invoked when a message arrives
	 * @return a handler registration
	 */
	Registration addMessageHandler(MessageHandler messageHandler);

}
