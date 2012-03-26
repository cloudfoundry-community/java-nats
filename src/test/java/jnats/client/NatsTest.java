package jnats.client;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class NatsTest {

	private static final int DEFAULT_START_PORT = 4000;

	private static final String SUBJECT = "test-subject";
	
	@Test
	public void connect() throws Exception {
		Process natsServer = startsNatsServer(DEFAULT_START_PORT);
		Nats nats = null;
		try {
			final CountDownLatch latch = new CountDownLatch(1);
			nats = new Nats.Builder()
					.addHost("nats://localhost:" + DEFAULT_START_PORT)
					.callback(new CallbackHandler() {
						@Override
						public void onConnect() {
							latch.countDown();
						}
					})
					.connect();
			Assert.assertTrue(latch.await(30, TimeUnit.SECONDS), "Connection attempt failed after 30 seconds.");
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
		new NatsTestCaseAwaitConnection() {
			@Override
			protected void connectedTest(Nats nats) throws Exception {
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
		final CountDownLatch closeLatch = new CountDownLatch(1);
		final CountDownLatch openLatch = new CountDownLatch(1);
		new NatsTestCase(DEFAULT_START_PORT, DEFAULT_START_PORT + 1) {

			@Override
			protected void test(Nats nats) throws Exception {
				final CountDownLatch latch = new CountDownLatch(1);
				Assert.assertTrue(openLatch.await(5, TimeUnit.SECONDS), "Didn't connect to server before timeout");
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
				nats.ping();
				Assert.assertTrue(closeLatch.await(5, TimeUnit.SECONDS), "Connection should have closed.");
				nats.publish(SUBJECT, "Test message");
				Assert.assertTrue(latch.await(10, TimeUnit.SECONDS), "After the server reconnected, either it failed to resubscribe or it failed to publish the pending message.");
			}

			@Override
			protected void modifyNatsConfig(Nats.Builder builder) {
				builder
					.reconnectWaitTime(1)
					.callback(new CallbackHandler() {
						@Override
						public void onClose() {
							closeLatch.countDown();
						}

						@Override
						public void onConnect() {
							openLatch.countDown();
						}
					});
			}
		};
	}

	@Test(dependsOnMethods = "simplePublishSubscribe")
	public void closeSubscription() throws Exception {
		new NatsTestCaseAwaitConnection() {
			@Override
			protected void connectedTest(Nats nats) throws Exception {
				final AtomicInteger messagesReceived = new AtomicInteger(0);
				final Subscription subscription = nats.subscribe(SUBJECT);
				subscription.addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						messagesReceived.incrementAndGet();
					}
				});
				nats.publish(SUBJECT, "First message");
				pingAndWait(nats);
				Assert.assertEquals(messagesReceived.get(), 1, "The first message didn't arrive.");
				subscription.close();
				nats.publish(SUBJECT, "Second message");
				pingAndWait(nats);
				Assert.assertEquals(messagesReceived.get(), 1, "The subscription didn't actually shut down, more than one message arrived.");
			}
		};
	}

	protected void pingAndWait(Nats nats) throws InterruptedException {
		final NatsFuture ping = nats.ping();
		Assert.assertTrue(ping.await(5, TimeUnit.SECONDS), "Timed out waiting for PONG from server.");
		Assert.assertTrue(ping.isSuccess());
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

	private static abstract class NatsTestCaseAwaitConnection extends NatsTestCase {
		
		private CountDownLatch latch;
		
		protected NatsTestCaseAwaitConnection(int... ports) throws Exception {
			super(ports);
		}

		@Override
		protected final void test(Nats nats) throws Exception {
			latch.await(5, TimeUnit.SECONDS);
			connectedTest(nats);
		}

		protected abstract void connectedTest(Nats nats) throws Exception;

		@Override
		protected final void modifyNatsConfig(Nats.Builder builder) {
			latch = new CountDownLatch(1);
			builder.callback(new CallbackHandler() {
				@Override
				public void onConnect() {
					latch.countDown();
				}
			});
		}
	}

}
