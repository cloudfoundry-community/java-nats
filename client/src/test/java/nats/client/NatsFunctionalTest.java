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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
@Test(groups = {"basic", "functional"})
public class NatsFunctionalTest {

	private NatsServerProcess natsServer;

	@BeforeSuite
	protected void startNatsServer() throws Exception {
		natsServer = new NatsServerProcess(4000);
		natsServer.start();
	}

	@AfterSuite
	protected void stopNatsServer() throws Exception {
		natsServer.stop();
	}

	@Test
	public void connectionTest() throws Exception {
		final Nats nats = new NatsConnector().addHost(natsServer.getUri()).connect();
		try {
			Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(2, TimeUnit.SECONDS));
		} finally {
			nats.close();
		}
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void connectAttemptWithNoHosts() {
		new NatsConnector().connect();
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void connectAttemptWithBadUri() {
		new NatsConnector().addHost("http://localhost").connect();
	}

	@Test
	public void blockingSubscribe() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) {
				final String subject = "test";
				final String message = "Have a nice day.";
				final Subscription subscription = nats.subscribe(subject);
				final SubscriptionIterator iterator = subscription.iterator();
				nats.publish(subject, message);
				Assert.assertEquals(iterator.next(1, TimeUnit.SECONDS).getBody(), message);
			}
		});
	}

	@Test
	public void nonBlockingSubscribe() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) throws Exception {
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
		});
	}

	@Test
	public void simpleRequestReply() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) throws Exception {
				final String subject = "test.request.subject";
				final CountDownLatch latch = new CountDownLatch(1);
				nats.subscribe(subject).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request: " + message);
						System.out.println("sending response");
						message.reply("Response");
					}
				});
				nats.request(subject, new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request response: " + message);
						latch.countDown();
					}
				});
				Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "Failed to get response from request within time limit.");
			}
		});
	}

	@Test
	public void closeSubscription() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) throws Exception {
				final String subject = "test.subscription.close";

				final Subscription subscription = nats.subscribe(subject);
				// Use the second subscription to make sure messages are being received but not being sent to the
				// closed subscription
				final Subscription subscription2 = nats.subscribe(subject);

				final CountDownLatch latch = new CountDownLatch(3);
				subscription2.addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						latch.countDown();
					}
				});

				nats.publish(subject, "First message").await();
				final SubscriptionIterator iterator = subscription.iterator();
				iterator.next(2, TimeUnit.SECONDS);
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The first message didn't arrive.");
				subscription.close();
				nats.publish(subject, "Second message").await();
				nats.publish(subject, "Third message").await();
				Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "Messages were not received on second subscription.");
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The subscription didn't actually shut down, more than one message arrived.");
			}
		});
	}

	@Test
	public void subscriptionEncoding() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) throws Exception {
				final String testString = "\uD834\uDD1E";
				final String subject = "test";
				final SubscriptionIterator iterator = nats.subscribe(subject).iterator();
				final Process process = new ProcessBuilder("nats-pub", "-s", natsServer.getUri(), subject, testString).start();
				Assert.assertEquals(process.waitFor(), 0, "Pub failed");
				final Message message = iterator.next(5, TimeUnit.SECONDS);
				Assert.assertNotNull(message, "Did not receive a message from server.");
				Assert.assertEquals(message.getBody(), testString);
			}
		});
	}

	protected void runNatsTest(NatsTestCase testCase) throws Exception {
		final Nats nats = new NatsConnector().addHost(natsServer.getUri()).debug(true).connect();
		Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(5, TimeUnit.SECONDS), "Did not connect to NATS server.");
		Assert.assertTrue(nats.getConnectionStatus().isConnected());
		try {
			testCase.natsTest(nats);
		} finally {
			nats.close();
		}
	}

	interface NatsTestCase {
		void natsTest(Nats nats) throws Exception;
	}
}
