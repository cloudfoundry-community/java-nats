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

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Provides the interface for publishing messages and subscribing to NATS subjects. This class is responsible for
 * maintaining a connection to the NATS server as well as automatic fail-over to a second server if the
 * connection to one server fails.
 * <p/>
 * <p>This class is fully thread-safe.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Nats extends Closeable {

	/**
	 * Indicates if this client is connected to a NATS server. This method will always return {@code false} after the
	 * {@link #close()} method has been invoked. If the connection to the server is lost, the client will automatically
	 * try to reconnect to a server unless, of course, this feature has been disabled.
	 *
	 * @return {@code true} if the client is connected to a NATS server, {@code false} otherwise.
	 */
	boolean isConnected();

	/**
	 * Indicates if this client has been closed.
	 *
	 * @return {@code true} if {@link #close()} was called (explicitly or implicitly due to an error), {@code false}
	 * otherwise.
	 */
	boolean isClosed();

	/**
	 * Closes this Nats instance. Closes the connection to the Nats server, closes any subscriptions, cancels any
	 * pending messages to be published.
	 */
	void close();

	/**
	 * Publishes a message with an empty body to the specified subject. If this {@code Nats} instance is not currently
	 * connected to a NATS server, the message will be queued up to be published once a connection is established.
	 *
	 * @param subject the subject to which the message will be published
	 */
	void publish(String subject);

	/**
	 * Publishes a message with the provided body to the specified subject. If this {@code Nats} instance is not
	 * currently connected to a NATS server, the message will be queued up to be published once a connection is
	 * established.
	 *
	 * @param subject the subject to which the message will be published
	 * @param body    the message body to publish
	 */
	void publish(String subject, String body);

	/**
	 * Publishes a message with the provided body to the specified subject. Any replies to this message will be sent to
	 * the specified reply to subject. If this {@code Nats} instance is not currently connected to a NATS server, the
	 * message will be queued up to be published once a connection is established.
	 *
	 * @param subject the subject to which the message will be published
	 * @param body    the message body to publish
	 * @param replyTo the subject replies to this body should be sent to.
	 */
	void publish(String subject, String body, String replyTo);

	/**
	 * Publishes a message with an empty body to the specified subject on a recurring basis according to the specified
	 * period. If this {@code Nats} instance is not currently connected to a NATS server, the message will not be sent
	 * or queued up.
	 *
	 * @param subject the subject to which the message will be published
	 * @param period the period between successive publishes
	 * @param unit the time unit of the period parameter
	 */
	Registration publish(String subject, long period, TimeUnit unit);

	/**
	 * Publishes a message with the provided body to the specified subject on a recurring basis according to the
	 * specified period. If this {@code Nats} instance is not currently connected to a NATS server, the message will
	 * not be sent or queued up.
	 *
	 * @param subject the subject to which the message will be published
	 * @param body    the message body to publish
	 * @param period the period between successive publishes
	 * @param unit the time unit of the period parameter
	 */
	Registration publish(String subject, String body, long period, TimeUnit unit);

	/**
	 * Publishes a message with the provided body to the specified subject. Any replies to this message will be sent to
	 * the specified reply to subject. If this {@code Nats} instance is not currently connected to a NATS server, the
	 * message will not be sent or queued up.
	 *
	 * @param subject the subject to which the message will be published
	 * @param body    the message body to publish
	 * @param replyTo the subject replies to this body should be sent to.
	 * @param period the period between successive publishes
	 * @param unit the time unit of the period parameter
	 */
	Registration publish(String subject, String body, String replyTo, long period, TimeUnit unit);

	/**
	 * Subscribes to the specified subject.
	 *
	 * @param subject the subject to subscribe to
	 * @return a {@code Subscription} object used for interacting with the subscription
	 * @see #subscribe(String, String, Integer,MessageHandler...)
	 */
	Subscription subscribe(String subject, MessageHandler... messageHandlers);

	/**
	 * Subscribes to the specified subject within a specific queue group. The subject can be a specific subject or
	 * include wild cards. A body to a particular subject will be delivered to only member of the same queue group.
	 *
	 * @param subject    the subject to subscribe to
	 * @param queueGroup the queue group the subscription participates in
	 * @return a {@code Subscription} object used for interacting with the subscription
	 * @see #subscribe(String, String, Integer,MessageHandler...)
	 */
	Subscription subscribe(String subject, String queueGroup, MessageHandler... messageHandlers);

	/**
	 * Subscribes to the specified subject and will automatically unsubscribe after the specified number of messages
	 * arrives.
	 *
	 * @param subject         the subject to subscribe to
	 * @param messageHandlers the {@code MessageHandler}s to listen for incoming messages.
	 * @return a {@code Subscription} object used for interacting with the subscription
	 * @see #subscribe(String, String, Integer,MessageHandler...)
	 */
	Subscription subscribe(String subject, Integer maxMessages, MessageHandler... messageHandlers);

	/**
	 * Subscribes to the specified subject within a specific queue group and will automatically unsubscribe after the
	 * specified number of messages arrives.
	 * <p/>
	 * <p>The {@code subject} may contain wild cards. "*" matches any token, at any level of the subject. For example:
	 * <pre>
	 *     "foo.*.baz"  matches "foo.bar.baz, foo.a.baz, etc.
	 *     "*.bar" matches "foo.bar", "baz.bar", etc.
	 *     "*.bar.*" matches "foo.bar.baz", "foo.bar.foo", etc.
	 * </pre>
	 * <p/>
	 * <p>">" matches any length of the tail of a subject and can only be the last token. For examples, 'foo.>' will
	 * match 'foo.bar', 'foo.bar.baz', 'foo.foo.bar.bax.22'. A subject of simply ">" will match all messages.
	 * <p/>
	 * <p>All subscriptions with the same {@code queueGroup} will form a queue group. Each body will be delivered to
	 * only one subscriber per queue group.
	 *
	 * @param subject         the subject to subscribe to
	 * @param queueGroup      the queue group the subscription participates in
	 * @param messageHandlers the {@code MessageHandler}s to listen for incoming messages.
	 * @return a {@code Subscription} object used for interacting with the subscription
	 */
	Subscription subscribe(String subject, String queueGroup, Integer maxMessages, MessageHandler... messageHandlers);

	/**
	 * Sends a request body on the given subject. Request responses can be handled using the returned
	 * {@link Request}.
	 *
	 * @param subject         the subject to send the request on
	 * @param message         the content of the request
	 * @param messageHandlers the {@code MessageHandler}s to listen for incoming messages.
	 * @return a {@code Request} instance associated with the request.
	 * @see #request(String, String, Integer, MessageHandler...)
	 */
	Request request(String subject, String message, MessageHandler... messageHandlers);

	/**
	 * Sends a request on the given subject with an empty body. Request responses can be handled using the returned
	 * {@link Request}.
	 *
	 * @param subject         the subject to send the request on
	 * @param messageHandlers the {@code MessageHandler}s to listen for incoming messages.
	 * @return a {@code Request} instance associated with the request.
	 * @see #request(String, String, Integer, MessageHandler...)
	 */
	Request request(String subject, MessageHandler... messageHandlers);

	/**
	 * Sends a request body on the given subject. Request responses can be handled using the returned
	 * {@link Request}.
	 * <p/>
	 * Invoking this method is roughly equivalent to the following:
	 * <p/>
	 * <code>
	 * String replyTo = Nats.createInbox();
	 * Subscription subscription = nats.subscribe(replyTo, maxReplies);
	 * Publication publication = nats.publish(subject, body, replyTo);
	 * </code>
	 * <p/>
	 * that returns a combination of {@code Subscription} and {@code Publication} as a {@code Request} object.
	 *
	 * @param subject         the subject to send the request on
	 * @param message         the content of the request
	 * @param maxReplies      the maximum number of replies that the request will accept before automatically closing,
	 *                        {@code null} for unlimited replies
	 * @param messageHandlers the {@code MessageHandler}s to listen for incoming messages.
	 * @return a {@code Request} instance associated with the request.
	 */
	Request request(String subject, String message, Integer maxReplies, MessageHandler... messageHandlers);

}
