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

import java.util.concurrent.TimeUnit;

/**
 * A NATS message.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Fix reply to behave the same way as CEB.
public interface Message {

	/**
	 * Returns {@code true} if the message originated from a request, {@code false} if the message originated from a
	 * publish. If the message originated from a request, the {@link #reply(String)} and
	 * {@link #reply(String, long, java.util.concurrent.TimeUnit)} may be used to reply to the request.
	 *
	 * @return {@code true} if the message originated from a request, {@code false} if the message originated from a
	 *         publish.
	 */
	boolean isRequest();

	/**
	 * Returns the subject used to send the message.
	 *
	 * @return the subject used to send the message.
	 */
	String getSubject();

	/**
	 * Returns the body of the message.
	 *
	 * @return the body of the message.
	 */
	String getBody();

	/**
	 * Returns the queue group of the message.
	 *
	 * @return the queue group of the message.
	 */
	String getQueueGroup();

	/**
	 * Sends a reply to this message. If the the message did not contain a {@code replyTo} field, a
	 * {@link nats.NatsException} will be thrown.
	 *
	 * @param message the message with which to reply to the requester
	 * @throws UnsupportedOperationException if the message did not originate from a request
	 */
	void reply(String message);

	/**
	 * Sends a reply to this message after the specified delay has passed. This method returns immediate and sends the
	 * delayed response asynchronously. If the the message did not contain a {@code replyTo} field, a
	 * {@link nats.NatsException} will be thrown.
	 *
	 * @param message the message with which to reply to the requester
	 * @param delay   the amount of time to wait before sending the reply
	 * @param unit    the time unit of the {@code delay} argument
	 * @throws UnsupportedOperationException if the message did not originate from a request
	 */
	void reply(String message, long delay, TimeUnit unit);

}
