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

/**
 * Represents a subscription timeout. Can be used to wait for the subscription to timeout or register a callback when
 * the subscription times out.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface SubscriptionTimeout {

	/**
	 * Block the current thread until the subscription times out.
	 */
	void await();

	/**
	 * Cancels the timeout.
	 *
	 * @return true if the timeout was cancelled, false if the timeout already occurred.
	 */
	boolean cancel();

	/**
	 * Returns the subscription that was timed out.
	 * @return the subscription that was timed out.
	 */
	Subscription getSubscription();

	/**
	 * Adds a handler to to be invoked when this subscription times out. The handler will not be invoked if this
	 * timeout is cancelled.
	 *
	 * @param timeoutHandler the handle to ve invoked
	 * @return a handler registration
	 */
	HandlerRegistration addTimeoutHandler(TimeoutHandler timeoutHandler);
}
