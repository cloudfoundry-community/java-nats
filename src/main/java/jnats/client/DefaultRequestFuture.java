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

import jnats.CompletionHandler;
import jnats.HandlerRegistration;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class DefaultRequestFuture implements RequestFuture {

	private final PublishFuture publishFuture;
	private final Subscription subscription;

	DefaultRequestFuture(Subscription subscription, PublishFuture publishFuture) {
		this.subscription = subscription;
		this.publishFuture = publishFuture;
	}

	@Override
	public String getMessage() {
		return publishFuture.getMessage();
	}

	@Override
	public String getSubject() {
		return publishFuture.getSubject();
	}

	@Override
	public String getReplyTo() {
		return publishFuture.getReplyTo();
	}

	@Override
	public HandlerRegistration addCompletionHandler(CompletionHandler handler) {
		return publishFuture.addCompletionHandler(handler);
	}

	@Override
	public boolean isDone() {
		return publishFuture.isDone();
	}

	@Override
	public boolean isSuccess() {
		return publishFuture.isSuccess();
	}

	@Override
	public Throwable getCause() {
		return publishFuture.getCause();
	}

	@Override
	public void await() throws InterruptedException {
		publishFuture.await();
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return publishFuture.await(timeout, unit);
	}

	@Override
	public void close() {
		subscription.close();
	}

	@Override
	public HandlerRegistration addMessageHandler(MessageHandler messageHandler) {
		return subscription.addMessageHandler(messageHandler);
	}

	@Override
	public int getReceivedMessages() {
		return subscription.getReceivedMessages();
	}

	@Override
	public Integer getMaxMessages() {
		return subscription.getMaxMessages();
	}

	@Override
	public String getQueueGroup() {
		return subscription.getQueueGroup();
	}

	@Override
	public SubscriptionIterator iterator() {
		return subscription.iterator();
	}
}
