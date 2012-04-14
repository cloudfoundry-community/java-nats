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

import nats.NatsFuture;

import java.util.concurrent.TimeUnit;

/**
 * A NATS message.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Message {

	/**
	 * Returns the {@link Subscription} instance the message arrived on.
	 *
	 * @return the {@link Subscription} instance the message arrived on
	 */
	Subscription getSubscription();

	/**
	 * Returns the NATS subject the message was published on.
	 *
	 * @return the NATS subject the message was published on
	 */
	String getSubject();

	/**
	 * Returns the body of the message.
	 *
	 * @return the body of the message.
	 */
	String getBody();

	/**
	 * Returns the reply to subject of the message or {@code null} if the message did not contain a {@code replyTo} field.
	 *
	 * @return the reply to subject of the message or {@code null} if the message did not contain a {@code replyTo} field
	 */
	String getReplyTo();

	/**
	 * Sends a reply to this message. If the the message did not contain a {@code replyTo} field, a
	 * {@link nats.NatsException} will be thrown.
	 *
	 * @param message the message with which to reply to the sender
	 * @return a {@code NatFuture} instance representing the pending reply operation
	 * @throws nats.NatsException if the message did not contain a {@code replyTo} field
	 */
	NatsFuture reply(String message);

	/**
	 * Sends a reply to this message after the specified delay has passed. This method returns immediate and sends the
	 * delayed response asynchronously. If the the message did not contain a {@code replyTo} field, a
	 * {@link nats.NatsException} will be thrown.
	 *
	 * @param message the message with which to reply to the sender
	 * @param delay the amount of time to wait before sending the reply
	 * @param unit the time unit of the {@code delay} argument
	 * @return a {@code NatFuture} instance representing the pending reply operation
	 * @throws nats.NatsException if the message did not contain a {@code replyTo} field
	 */
	NatsFuture reply(String message, long delay, TimeUnit unit);

}
