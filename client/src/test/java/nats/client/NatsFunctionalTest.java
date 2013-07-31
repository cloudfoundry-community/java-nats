/*
 *   Copyright (c) 2012,2013 Mike Heath.  All rights reserved.
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

import static org.testng.Assert.*;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
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
		final BlockingConnectionStateListener listener = new BlockingConnectionStateListener();
		try (Nats nats = new NatsConnector().addHost(natsServer.getUri()).addConnectionStateListener(listener).connect()) {
			listener.awaitReady();
			assertTrue(nats.isConnected());
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
				final MessageIterator iterator = subscription.iterator();
				nats.publish(subject, message);
				final Message next = iterator.next(2, TimeUnit.SECONDS);
				assertNotNull(next);
				assertEquals(next.getBody(), message);
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
				assertTrue(latch.await(1, TimeUnit.SECONDS));
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
				nats.request(subject, "", 1, TimeUnit.MINUTES, new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request response: " + message);
						latch.countDown();
					}
				});
				assertTrue(latch.await(5, TimeUnit.SECONDS), "Failed to get response from request within time limit.");
			}
		});
	}

	@Test
	public void requestAutoClose() throws Exception {
		runNatsTest(new NatsTestCase() {
			@Override
			public void natsTest(Nats nats) throws Exception {
				final ExecutorService executorService = Executors.newCachedThreadPool();
				try {
					final String subject = "some.request.subject";
					final AtomicInteger counter = new AtomicInteger();

					nats.subscribe(subject, new MessageHandler() {
						@Override
						public void onMessage(final Message message) {
							executorService.execute(new Runnable() {
								@Override
								public void run() {
									while (!Thread.currentThread().isInterrupted()) {
										message.reply("Your mom goes to college.");
										try {
											Thread.sleep(100); // Sleep for 1/10 second
										} catch (InterruptedException e) {
											// We've been interrupted, we're done.
										}
									}
								}
							});
						}
					});

					nats.request(subject, "", 1, TimeUnit.SECONDS, new MessageHandler() {
						@Override
						public void onMessage(Message message) {
							counter.incrementAndGet();
						}
					});

					// Sleep for 2 seconds, the request should only be open for 1, it should get 10 replies.
					Thread.sleep(2000);

					assertEquals(10, counter.get());
				} finally {
					executorService.shutdownNow();
				}
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

				nats.publish(subject, "First message");
				final MessageIterator iterator = subscription.iterator();
				iterator.next(2, TimeUnit.SECONDS);
				assertEquals(subscription.getReceivedMessages(), 1, "The first message didn't arrive.");
				subscription.close();
				nats.publish(subject, "Second message");
				nats.publish(subject, "Third message");
				assertTrue(latch.await(5, TimeUnit.SECONDS), "Messages were not received on second subscription.");
				assertEquals(subscription.getReceivedMessages(), 1, "The subscription didn't actually shut down, more than one message arrived.");
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
				final MessageIterator iterator = nats.subscribe(subject).iterator();
				final Process process = new ProcessBuilder("nats-pub", "-s", natsServer.getUri(), subject, testString).start();
				assertEquals(process.waitFor(), 0, "Pub failed");
				final Message message = iterator.next(5, TimeUnit.SECONDS);
				assertNotNull(message, "Did not receive a message from server.");
				assertEquals(message.getBody(), testString);
			}
		});
	}

	protected void runNatsTest(NatsTestCase testCase) throws Exception {
		final BlockingConnectionStateListener listener = new BlockingConnectionStateListener();
		try (final Nats nats = new NatsConnector().addHost(natsServer.getUri()).addConnectionStateListener(listener).connect()) {
			listener.awaitReady();
			assertTrue(nats.isConnected());
			testCase.natsTest(nats);
		}
	}

	interface NatsTestCase {
		void natsTest(Nats nats) throws Exception;
	}
}
