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
 * Provides a mechanism for removing handlers such as a {@link MessageHandler} or
 * {@link nats.client.MessageHandler} or to cancel recurring publishes.
 *
 * <p>For example, when using a {@link nats.client.Subscription}, you may add a {@link nats.client.MessageHandler}
 * using the {@link nats.client.Subscription#addMessageHandler(nats.client.MessageHandler)} method. This method returns
 * an instance of {@code Registration}. When the {@link #remove()} method is invoked, the {@code MessageHandler} is
 * removed from the {@code Subscription} and will no longer be called when new messages arrive on the
 * {@code Subscription}.
 *
 * @author Mike Heath
 */
public interface Registration {

	/**
	 * Removes the handler associated with this {@code Registration}.
	 */
	void remove();

}
