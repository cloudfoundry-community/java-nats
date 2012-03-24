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

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface NatsFuture {

	HandlerRegistration addCompletionHandler(CompletionHandler listener);

	/**
	 * Returns {@code true} if and only if the message publishing or pinging is complete, regardless of whether
	 * sending the message/ping was successful.
	 */
	boolean isDone();

	/**
	 * Returns {@code true} if and only if the message was sent to the Nats server or in the case of a ping, returns
	 * true if a pong message arrived..
	 */
	boolean isSuccess();

	/**
	 * Returns the cause of the failed operation assuming the operation has failed.
	 *
	 * @return the cause of the failure.
	 *         {@code null} if succeeded or this future is not
	 *         completed yet.
	 */
	Throwable getCause();

	/**
	 * Waits for published message to be sent.
	 *
	 * @throws InterruptedException if the current thread was interrupted
	 */
	void await() throws InterruptedException;

	/**
	 * Waits for the published message to be sent within the specified time limit.
	 *
	 * @return {@code true} if and only if the future was completed within
	 *         the specified time limit
	 *
	 * @throws InterruptedException
	 *         if the current thread was interrupted
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;


}
