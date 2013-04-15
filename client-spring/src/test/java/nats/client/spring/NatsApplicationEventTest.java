/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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
package nats.client.spring;

import nats.client.Nats;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsApplicationEventTest {

	public static void main(String[] args) throws Exception {
		// TODO Make automated test that starts a NATS server and then runs test.

		// Nats server must running before running this test.
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("natsApplicationEventContext.xml");
		try {
			final Nats nats = context.getBean(Nats.class);
			Assert.assertNotNull(nats);

			final boolean invokedConnected = context.getBean("connected", ConnectListener.class).invoked.await(2, TimeUnit.SECONDS);
			Assert.assertTrue(invokedConnected, "The connected application event was never published.");
			final boolean invokedReady = context.getBean("ready", ReadyListener.class).invoked.await(2, TimeUnit.SECONDS);
			Assert.assertTrue(invokedReady, "The server ready application event was never published.");
			nats.close();
			final boolean invokedClosed = context.getBean("closed", ClosedListener.class).invoked.await(2, TimeUnit.SECONDS);
			Assert.assertTrue(invokedClosed, "The closed application event was never published.");
		} finally {
			context.close();
		}
	}

	public static class ConnectListener implements ApplicationListener<NatsConnectedApplicationEvent> {

		final CountDownLatch invoked = new CountDownLatch(1);

		@Override
		public void onApplicationEvent(NatsConnectedApplicationEvent event) {
			invoked.countDown();
		}
	}

	public static class ReadyListener implements ApplicationListener<NatsServerReadyApplicationEvent> {

		final CountDownLatch invoked = new CountDownLatch(1);

		@Override
		public void onApplicationEvent(NatsServerReadyApplicationEvent event) {
			invoked.countDown();
		}
	}

	public static class ClosedListener implements ApplicationListener<NatsClosedApplicationEvent> {

		final CountDownLatch invoked = new CountDownLatch(1);

		@Override
		public void onApplicationEvent(NatsClosedApplicationEvent event) {
			invoked.countDown();
		}
	}
}
