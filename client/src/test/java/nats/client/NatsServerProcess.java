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

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsServerProcess {

	private static final long NATS_SERVER_STARTUP_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private final int port;
	private final String userName;
	private final String password;

	private Process process;

	public NatsServerProcess(int port) {
		this(port, null, null);
	}

	public NatsServerProcess(int port, String userName, String password) {
		this.port = port;
		this.userName = userName;
		this.password = password;
	}

	public void start() throws IOException {
		if (process != null) {
			throw new IllegalStateException("Server has already been started.");
		}
		if (!isPortAvailable(port)) {
			throw new IllegalStateException("Something is already listening on port " + port);
		}
		final List<String> command = new ArrayList<String>();
		command.add("nats-server");
		command.add("-p");
		command.add(Integer.toString(port));
		if (userName != null) {
			command.add("--user");
			command.add(userName);
		}
		if (password != null) {
			command.add("--pass");
			command.add(password);
		}
		final ProcessBuilder builder = new ProcessBuilder(command.toArray(new String[command.size()]));
		process = builder.start();
		waitForPort();
	}

	private static boolean isPortAvailable(int port) {
		try {
			new Socket("localhost", port).close();
			return false;
		} catch (IOException e) {
			return true;
		}

	}

	public void stop() throws InterruptedException {
		if (process == null) {
			throw new IllegalStateException("Server was never started.");
		}
		process.destroy();
		process.waitFor();
	}

	public int getPort() {
		return port;
	}

	public static void main(String[] args) throws Exception {
		final NatsServerProcess natsServerProcess = new NatsServerProcess(4000);
		natsServerProcess.start();
		System.out.println("Nats server started.");
		Thread.sleep(5000);
		natsServerProcess.stop();
		System.out.println("Nats server stopped.");
	}

	private void waitForPort() {
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < NATS_SERVER_STARTUP_TIMEOUT) {
			if (!isPortAvailable(port)) {
				return;
			}
			Thread.yield();
		}
		throw new RuntimeException("NATS server failed to start.");
	}

	public String getUri() {
		final StringBuilder builder = new StringBuilder();
		builder.append("nats://");
		if (userName != null) {
			builder.append(userName).append(":").append(password).append("@");
		}
		builder.append("localhost:").append(port);
		return builder.toString();
	}
}
