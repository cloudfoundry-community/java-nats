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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provide a mock instance of {@link Nats} to use primarily for testing purposes. This mock Nats does not yet support
 * subscribing to subjects with wild cards.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockNats implements Nats {

	private volatile boolean connected = true;
	private final Map<String, Collection<DefaultSubscription>> subscriptions = new HashMap<>();

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public boolean isClosed() {
		return !connected;
	}

	@Override
	public void close() {
		connected = false;
	}

	@Override
	public void publish(String subject) {
		publish(subject, null);
	}

	@Override
	public void publish(String subject, String body) {
		publish(subject, body, null);
	}

	@Override
	public void publish(String subject, String body, String replyTo) {
		final Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(subject);
		if (mockSubscriptions != null) {
			for (DefaultSubscription subscription : mockSubscriptions) {
				subscription.onMessage(subject, body, replyTo);
			}
		}
	}

	@Override
	public Subscription subscribe(String subject) {
		return subscribe(subject, null, null);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup) {
		return subscribe(subject, queueGroup, null);
	}

	@Override
	public Subscription subscribe(String subject, Integer maxMessages) {
		return subscribe(subject, null, maxMessages);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup, Integer maxMessages) {
		final DefaultSubscription subscription = new DefaultSubscription(subject, queueGroup, maxMessages);
		Collection<DefaultSubscription> mockSubscriptions = subscriptions.get(subject);
		if (mockSubscriptions == null) {
			mockSubscriptions = new ArrayList<>();
			subscriptions.put(subject, mockSubscriptions);
		}
		mockSubscriptions.add(subscription);
		return subscription;
	}

	@Override
	public Request request(String subject, String message, MessageHandler... messageHandlers) {
		return null;
	}

	@Override
	public Request request(String subject, MessageHandler... messageHandlers) {
		return null;
	}

	@Override
	public Request request(String subject, String message, Integer maxReplies, MessageHandler... messageHandlers) {
		return null;
	}

}
