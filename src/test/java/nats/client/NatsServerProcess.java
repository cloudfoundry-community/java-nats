package nats.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @author Mike Heath <heathma@ldschurch.org>
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
