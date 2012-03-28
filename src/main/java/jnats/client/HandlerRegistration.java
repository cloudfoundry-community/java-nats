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

/**
 * Provides a registration for a handler such as a {@link CompletionHandler} or {@link MessageHandler}. This
 * registration is used to remove the handler from the list of handlers that will be invoked when the pending operation
 * completes.
 * 
 * <p>For example, when using a {@link Subscription}, you may add a {@link MessageHandler} using the
 * {@link Subscription#addMessageHandler(MessageHandler)} method. This method returns an instance of
 * {@code HandlerRegistration}. When the {@link #remove()} method is invoked, the {@code MessageHandler} is removed
 * from the {@code Subscription} and will no longer be called when new messages arrive on the {@code Subscription}.
 * 
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface HandlerRegistration {

	/**
	 * Removes the handler associated with this {@code HandlerRegistration}.
	 */
	void remove();

}
