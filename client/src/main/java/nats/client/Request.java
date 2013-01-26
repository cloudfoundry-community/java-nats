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

/**
 * Holds the attributes associated with a request.
 *
 * <p>This interface is similar to {@link Subscription}. However, it has some important differences. Instances of
 * {@code Subscription} are {@link Iterable}; requests are not. A request cannot be iterable. If a reply to the request
 * is received before the {@link Iterable#iterator()} method is invoked, the reply will get lost. If the replies are
 * queued and {@link Iterable#iterator()} is never invoked, memory will be wasted.
 *
 * <p>Additionally, {@code Subscription} has a {@link Subscription#addMessageHandler(MessageHandler)} method for adding
 * {@link MessageHandler}s after the subscription has been created. Similar to the previous problem, if a reply is
 * received before the {@code MessageHandler} has been added to the request, receipt of the message will be lost.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Request extends AutoCloseable {

	/**
	 * Closes this request. Any {@link MessageHandler} objects associated with this request will no longer receive any
	 * messages after this method is invoked.
	 */
	@Override
	void close();

	/**
	 * Returns the subject the request was sent on.
	 *
	 * @return the subject the request was sent on.
	 */
	String getSubject();

	/**
	 * Returns the number of replies this request has received.
	 *
	 * @return the number of replies this request has received.
	 */
	int getReceivedReplies();

	/**
	 * Returns the maximum number of replies this request will receive.
	 *
	 * @return the maximum number of replies this request will receive or {@code null} if no maximum was specified.
	 */
	Integer getMaxReplies();

}
