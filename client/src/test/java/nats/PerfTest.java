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

import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.MessageIterator;
import nats.client.Nats;
import nats.client.NatsConnector;
import nats.client.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class PerfTest {
	public static void main(String[] args) throws Exception {
		final NatsConnector builder = new NatsConnector().addHost("nats://localhost");
		final Nats sender = builder.connect();
		for (int i = 100; i <= 1000; i += 100) {
			final Collection<Nats> connections = new ArrayList<Nats>(i);
			for (int j = 0; j < i; j++) {
				connections.add(builder.connect());
			}
			// Warm up.
			System.out.println("Warming up");
			time(sender, "warmup", connections, 1000);
			Thread.sleep(1000);
			System.out.println("Warming up 2");
			time(sender, "warmup2", connections, 1000);
			Thread.sleep(1000);
			System.out.println("Running test.");
			long time = time(sender, "test.load", connections, 1);
			System.out.printf("Time for %d connections, %d\n", i, time);
			for (Nats nats : connections) {
				nats.close();
			}
		}
	}

	private static long time(Nats sender, String subject, Collection<Nats> connections, int messageCount) throws Exception {
		final CountDownLatch latch = new CountDownLatch(connections.size() * messageCount);
		final String message = createMessage(1024);
		final MessageHandler messageHandler = new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				latch.countDown();
			}
		};
		for (Nats nats : connections) {
			nats.subscribe(subject).addMessageHandler(messageHandler);
		}
		Thread.sleep(1000);
		long start = System.currentTimeMillis();
		for (int i = 0; i < messageCount; i++) {
			sender.publish(subject, message);
		}
		latch.await();
		return System.currentTimeMillis() - start;
	}

	private static String createMessage(int size) {
		final String alphabet = "abcdefghijklmnopqrstuvwxyz";
		char[] message = new char[size];
		for (int i = 0; i < size; i++) {
			message[i] = alphabet.charAt(i % alphabet.length());
		}
		return new String(message);
	}

	private static void blockUntilConnected(Collection<Nats> connections) {
		for (Nats nats : connections) {
			final Subscription subscription = nats.subscribe("test");
			final MessageIterator iterator = subscription.iterator();
			nats.publish("test", "Have a nice day.");
			iterator.next();
			subscription.close();
		}
	}
}
