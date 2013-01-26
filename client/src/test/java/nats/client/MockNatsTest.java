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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockNatsTest {

	@Test
	public void blockingSubscribe() {
		final Nats nats = new MockNats();
		final String subject = "test";
		final String message = "Have a nice day.";
		final Subscription subscription = nats.subscribe(subject);
		final MessageIterator iterator = subscription.iterator();
		nats.publish(subject, message);
		Assert.assertEquals(iterator.next(1, TimeUnit.SECONDS).getBody(), message);
	}

	@Test
	public void blockingSubscribeNullMessage() {
		final Nats nats = new MockNats();
		final String subject = "test";
		final Subscription subscription = nats.subscribe(subject);
		final MessageIterator iterator = subscription.iterator();
		nats.publish(subject);
		Assert.assertNull(iterator.next().getBody());
	}

	@Test
	public void nonBlockingSubscribe() throws Exception {
		final Nats nats = new MockNats();
		final String subject = "test";
		final String message = "Have a nice day.";
		final CountDownLatch latch = new CountDownLatch(1);
		nats.subscribe(subject).addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				latch.countDown();
			}
		});
		nats.publish(subject, message);
		Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
	}


}
