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

import nats.NatsLogger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsTest {

	private static final int DEFAULT_START_PORT = 4000;

	private static final String SUBJECT = "test-subject";
	
	@Test
	public void connect() throws Exception {
		Process natsServer = startsNatsServer(DEFAULT_START_PORT);
		Nats nats = null;
		try {
			nats = new Nats.Builder()
					.addHost("nats://localhost:" + DEFAULT_START_PORT)
					.connect();
			Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(30, TimeUnit.SECONDS), "Connection attempt failed after 30 seconds.");
		} finally {
			natsServer.destroy();
			if (nats != null) {
				nats.close();
			}
		}
	}

	@Test
	public void connectAttemptWithNoHosts() {
		try {
			new Nats.Builder().connect();
			Assert.fail("An exception should have been thrown indicating no hosts were specified.");
		} catch (IllegalStateException e) {
			// Pass
		}
	}

	@Test(dependsOnMethods = "connect")
	public void simplePublishSubscribe() throws Exception {
		new NatsTestCase() {
			@Override
			protected void test(Nats nats) throws Exception {
				final CountDownLatch latch = new CountDownLatch(1);
				nats.subscribe(SUBJECT).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						latch.countDown();
					}
				});
				nats.publish(SUBJECT, "Test message");
				Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "The published message was not received in the specified amount of time.");
			}
		};
	}

	@Test(dependsOnMethods = "simplePublishSubscribe")
	public void simpleRequestReply() throws Exception {
		new NatsTestCase() {
			@Override
			protected void test(Nats nats) throws Exception {
				Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(10, TimeUnit.SECONDS), "Failed to connect to server.");

				final CountDownLatch latch = new CountDownLatch(1);
				nats.subscribe(SUBJECT).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request, sending reply");
						message.reply("Reply");
					}
				});
				nats.request(SUBJECT, "Request").addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request response");
						latch.countDown();
					}
				});
				Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "Failed to get response from request within time limit.");
			}
		};
	}
	
	@Test(dependsOnMethods = "simplePublishSubscribe")
	public void resubscribeAfterServerKill() throws Exception {
//		final CountDownLatch closeLatch = new CountDownLatch(1);
//		final CountDownLatch openLatch = new CountDownLatch(1);
//		new NatsTestCase(DEFAULT_START_PORT, DEFAULT_START_PORT + 1) {
//
//			@Override
//			protected void test(Nats nats) throws Exception {
//				final CountDownLatch latch = new CountDownLatch(1);
//				Assert.assertTrue(openLatch.await(5, TimeUnit.SECONDS), "Didn't connect to server before timeout");
//				nats.subscribe(SUBJECT).addMessageHandler(new MessageHandler() {
//					@Override
//					public void onMessage(Message message) {
//						latch.countDown();
//					}
//				});
//				// Kill server we're connected to
//				natsServers[0].destroy();
//				System.out.println("Waiting for server to shutdown...");
//				natsServers[0].waitFor();
//				System.out.println("Server shutdown.");
//				Assert.assertTrue(closeLatch.await(5, TimeUnit.SECONDS), "Connection should have closed.");
//				nats.publish(SUBJECT, "Test message");
//				Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "After the server reconnected, either it failed to resubscribe or it failed to publish the pending message.");
//			}
//
//			@Override
//			protected void modifyNatsConfig(Nats.Builder builder) {
//				builder
//					.reconnectWaitTime(1, TimeUnit.SECONDS)
//					.callback(new ExceptionHandler() {
//						@Override
//						public void onClose() {
//							closeLatch.countDown();
//						}
//
//						@Override
//						public void onConnect() {
//							openLatch.countDown();
//						}
//					});
//			}
//		};
	}

	@Test(dependsOnMethods = "simplePublishSubscribe", timeOut = 10000)
	public void closeSubscription() throws Exception {
		new NatsTestCase() {
			@Override
			protected void test(Nats nats) throws Exception {
				Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(10, TimeUnit.SECONDS), "Failed to connect to server.");

				final Subscription subscription = nats.subscribe(SUBJECT);
				nats.publish(SUBJECT, "First message").await();
				nats.publish("ping", "Flush things through the server.").await(); // TODO Maybe we do re-enable pings.
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The first message didn't arrive.");
				subscription.close();
				nats.publish(SUBJECT, "Second message").await();
				nats.publish("ping", "Flush things through the server.").await();
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The subscription didn't actually shut down, more than one message arrived.");
			}
		};
	}

	@Test(dependsOnMethods = "simplePublishSubscribe")
	public void blockingSubscription() throws Exception {
		new NatsTestCase() {
			@Override
			protected void test(Nats nats) throws Exception {
				final Subscription subscription = nats.subscribe(SUBJECT);
				final SubscriptionIterator iterator = subscription.iterator();
				Assert.assertNull(iterator.next(1, TimeUnit.SECONDS));
				final String message = "Blocking test.";
				nats.publish(SUBJECT, message);
				Assert.assertEquals(iterator.next().getBody(), message);
			}
		};
	}

	static Process startsNatsServer(int port) throws IOException {
		ProcessBuilder builder = new ProcessBuilder("nats-server", "-p", Integer.toString(port)).inheritIO();
		final Process process = builder.start();
		boolean done = false;
		while (!done) {
			try {
				new Socket("localhost", port).close();
				done = true;
			} catch (IOException e) {
			}
		}
		System.out.println("Nats server started on port " + port);
		return process;
	}
	
	private static abstract class NatsTestCase {

		protected final Process[] natsServers;

		public NatsTestCase(int... ports) throws Exception {
			if (ports.length == 0) {
				ports = new int[] {DEFAULT_START_PORT};
			}
			Nats.Builder builder = new Nats.Builder();
			natsServers = new Process[ports.length];
			for (int i = 0; i < ports.length; i++) {
				final int port = ports[i];
				natsServers[i] = startsNatsServer(port);
				builder.addHost("nats://localhost:" + port);
			}
			builder.logger(new NatsLogger() {

				@Override
				public void log(Level level, String message) {
					System.out.println(level.toString() + ": " + message);
				}

				@Override
				public void log(Level level, Throwable t) {
					t.printStackTrace();
				}
			});
			modifyNatsConfig(builder);
			Nats nats = builder.connect();
			try {
				test(nats);
			} finally {
				for (int i = 0; i < ports.length; i++) {
					final Process natsServer = natsServers[i];
					natsServer.destroy();
					natsServer.waitFor();
				}
				nats.close();
			}
			
		}
		
		protected void modifyNatsConfig(Nats.Builder builder) {}
		
		protected abstract void test(Nats nats) throws Exception;
	}

}
