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
public class NatsIT {

	private static final int DEFAULT_START_PORT = 4000;

	private static final String SUBJECT = "test-subject";
	
//	@Test
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

//	@Test
	public void connectAttemptWithNoHosts() {
		try {
			new Nats.Builder().connect();
			Assert.fail("An exception should have been thrown indicating no hosts were specified.");
		} catch (IllegalStateException e) {
			// Pass
		}
	}

//	@Test
	public void disableReconnectWithNoRunningServers() throws Exception {
		final Nats nats = new Nats.Builder().addHost("nats://1.2.3.4").automaticReconnect(false).connect();
		nats.getConnectionStatus().awaitConnectionClose(10, TimeUnit.SECONDS);
		Assert.assertFalse(nats.getConnectionStatus().isConnected());
		Assert.assertFalse(nats.getConnectionStatus().isServerReady());
	}

//	@Test(dependsOnMethods = "connect")
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

//	@Test(dependsOnMethods = "simplePublishSubscribe")
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
	
//	@Test(dependsOnMethods = "simplePublishSubscribe")
	public void resubscribeAfterServerKill() throws Exception {
		new NatsTestCase(DEFAULT_START_PORT, DEFAULT_START_PORT + 1) {

			@Override
			protected void test(Nats nats) throws Exception {
				final CountDownLatch latch = new CountDownLatch(1);
				Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(5, TimeUnit.SECONDS), "Didn't connect to server before timeout");
				nats.subscribe(SUBJECT).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						latch.countDown();
					}
				});
				// Kill server we're connected to
				natsServers[0].destroy();
				System.out.println("Waiting for server to shutdown...");
				natsServers[0].waitFor();
				System.out.println("Server shutdown.");
				Assert.assertTrue(nats.getConnectionStatus().awaitConnectionClose(5, TimeUnit.SECONDS), "Connection should have closed.");
				nats.publish(SUBJECT, "Test message");
				Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "After the server reconnected, either it failed to resubscribe or it failed to publish the pending message.");
			}

			@Override
			protected void modifyNatsConfig(Nats.Builder builder) {
				builder.reconnectWaitTime(5, TimeUnit.SECONDS);
			}
		};
	}

//	@Test(dependsOnMethods = "simplePublishSubscribe", timeOut = 10000)
	public void closeSubscription() throws Exception {
		new NatsTestCase() {
			@Override
			protected void test(Nats nats) throws Exception {
				Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(10, TimeUnit.SECONDS), "Failed to connect to server.");

				final Subscription subscription = nats.subscribe(SUBJECT);
				 // Use the second subscription to make sure messages are being received but not being sent to the
				 // closed subscription
				final Subscription subscription2 = nats.subscribe(SUBJECT);

				final CountDownLatch latch = new CountDownLatch(3);
				subscription2.addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						latch.countDown();
					}
				});

				nats.publish(SUBJECT, "First message").await();
				final SubscriptionIterator iterator = subscription.iterator();
				iterator.next(2, TimeUnit.SECONDS);
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The first message didn't arrive.");
				subscription.close();
				nats.publish(SUBJECT, "Second message").await();
				nats.publish(SUBJECT, "Third message").await();
				Assert.assertTrue(latch.await(5, TimeUnit.SECONDS), "Messages were not received on second subscription.");
				Assert.assertEquals(subscription.getReceivedMessages(), 1, "The subscription didn't actually shut down, more than one message arrived.");
			}
		};
	}

//	@Test(dependsOnMethods = "simplePublishSubscribe")
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

	static Process startsNatsServer(int port) throws IOException, InterruptedException {
		// Make sure previous server has shutdown.
		boolean done = false;
		System.out.println("Waiting for server on port " + port + " to shutdown.");
		while (!done) {
			try {
				new Socket("localhost", port).close();
				Thread.sleep(100);
			} catch (IOException e) {
				done = true;
			}
		}
		System.out.println("Done!");

		ProcessBuilder builder = new ProcessBuilder("nats-server", "-p", Integer.toString(port));
		final Process process = builder.start();
		done = false;
		System.out.println("Waiting for nats server to start up on port " + port);
		while (!done) {
			try {
				new Socket("localhost", port).close();
				done = true;
			} catch (IOException e) {
				Thread.sleep(100);
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
			builder.logger(NatsLogger.DEBUG_LOGGER);
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
		
		protected void modifyNatsConfig(Nats.Builder builder) {
			// Do nothing.
		}
		
		protected abstract void test(Nats nats) throws Exception;
	}

}
