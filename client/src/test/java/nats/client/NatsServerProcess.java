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

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsServerProcess {

	private final int port;

	private Process process;

	public NatsServerProcess(int port) {
		this.port = port;
	}

	public void start() throws IOException {
		if (process != null) {
			throw new IllegalStateException("Server has already been started.");
		}
		if (!isPortAvailable(port)) {
			throw new IllegalStateException("Something is already listening on port " + port);
		}
		ProcessBuilder builder = new ProcessBuilder("nats-server", "-p", Integer.toString(port));
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
		while (true) {
			if (!isPortAvailable(4000)) {
				return;
			}
			Thread.yield();
		}
	}

	public String getUri() {
		return "nats://localhost:" + port;
	}
}
