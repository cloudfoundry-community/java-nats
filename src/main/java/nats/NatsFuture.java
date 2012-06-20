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
package nats;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pending Nats operation. Most operations in Nats are asynchronous meaning that method invocations will
 * return immediately regardless of whether the operation has completed or not. Instances of this class can be used to
 * track these pending operations and determine if they have completed successfully.
 * 
 * <p>{@link #addCompletionHandler(CompletionHandler)}</p> can be used to get notified when the pending operation has
 * completed. Alternatively, {@link #await()} or {@link #await(long, java.util.concurrent.TimeUnit)} can be used to
 * block the current thread's execution until the operation completes.
 * 
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface NatsFuture {

	/**
	 * Adds the specified handler to this future.  The specified handler is invoked when this future has completed,
	 * successful or not.  If this future is already completed, the specified listener is invoked immediately.
	 *
	 * @param handler the handler to invoke when this future object has completed
	 * @return a handler registration
	 */
	HandlerRegistration addCompletionHandler(CompletionHandler handler);

	/**
	 * Returns {@code true} if and only if the pending operation is complete, regardless of whether the operation
	 * completed successfully.
	 *
	 * @return true if the future has completed, false otherwise
	 */
	boolean isDone();

	/**
	 * Returns {@code true} if and only if the pending operation completed successfully.
	 *
	 * @return true of the future completed successfully, false otherwise.
	 */
	boolean isSuccess();

	/**
	 * Returns the cause of the failed operation assuming the operation has failed.
	 *
	 * @return the cause of the failure.
	 *         {@code null} if succeeded or this future is not completed yet.
	 */
	Throwable getCause();

	/**
	 * Waits for the pending operation to complete.
	 *
	 * @throws InterruptedException if the current thread was interrupted
	 */
	void await() throws InterruptedException;

	/**
	 * Waits for the pending operation to complete within the specified time limit.
	 *
	 * @return {@code true} if and only if the future was completed within
	 *         the specified time limit
	 *
	 * @param timeout the maximum amount of time to wait for the operation to complete
	 * @param unit the unit of the {@code timeout} argument
	 * @throws InterruptedException
	 *         if the current thread was interrupted
	 */
	boolean await(long timeout, TimeUnit unit) throws InterruptedException;


}
