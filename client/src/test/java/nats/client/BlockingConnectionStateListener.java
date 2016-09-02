package nats.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;

/**
 * @author Mike Heath
 */
public class BlockingConnectionStateListener implements ConnectionStateListener {

	private final CountDownLatch connectLatch;
	private final CountDownLatch disconnectLatch;
	private final CountDownLatch readyLatch;

	public BlockingConnectionStateListener(int latchCount) {
		connectLatch = new CountDownLatch(latchCount);
		disconnectLatch = new CountDownLatch(latchCount);
		readyLatch = new CountDownLatch(latchCount);
	}

	public BlockingConnectionStateListener() {
		this(1);
	}

	@Override
	public void onConnectionStateChange(Nats nats, State state) {
		switch (state) {
			case CONNECTED:
				connectLatch.countDown();
				break;
			case DISCONNECTED:
				disconnectLatch.countDown();
				break;
			case SERVER_READY:
				readyLatch.countDown();
				break;
		}
	}

	public void awaitConnect() throws InterruptedException {
		Assert.assertTrue(connectLatch.await(10, TimeUnit.SECONDS));
	}

	public void awaitDisconnect() throws InterruptedException {
		Assert.assertTrue(disconnectLatch.await(10, TimeUnit.SECONDS));
	}

	public void awaitReady() throws InterruptedException {
		Assert.assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
	}
}
