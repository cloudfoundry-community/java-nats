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

import java.util.concurrent.TimeUnit;

/**
 * Tests authentication against a secured NATS server.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsAuthenticationTest {

	private static final int PORT = 4001;

	private static final String USER_NAME = "joeuser";
	private static final String PASSWORD = "joespassword";

	private NatsServerProcess natsServer;

	@BeforeSuite
	protected void startNatsServer() throws Exception {
		natsServer = new NatsServerProcess(PORT, USER_NAME, PASSWORD);
		natsServer.start();
	}

	@AfterSuite
	protected void stopNatsServer() throws Exception {
		natsServer.stop();
	}

	@Test
	public void failedAuthenticationTest() throws Exception {
		final Nats nats = new NatsConnector().addHost("nats://localhost:" + PORT).debug(true).automaticReconnect(false).connect();
		try {
			Assert.assertFalse(nats.getConnectionStatus().awaitServerReady(5, TimeUnit.SECONDS), "Did not connect to NATS server.");
			Assert.assertFalse(nats.getConnectionStatus().isConnected());
		} finally {
			nats.close();
		}
	}

	@Test
	public void successfulAuthenticationTest() throws Exception {
		final Nats nats = new NatsConnector().addHost(natsServer.getUri()).debug(true).automaticReconnect(false).connect();
		try {
			Assert.assertTrue(nats.getConnectionStatus().awaitServerReady(5, TimeUnit.SECONDS), "Did not connect to NATS server.");
			Assert.assertTrue(nats.getConnectionStatus().isConnected());
		} finally {
			nats.close();
		}
	}
}
