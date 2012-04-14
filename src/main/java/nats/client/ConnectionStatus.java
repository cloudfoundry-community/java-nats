package nats.client;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public interface ConnectionStatus {

	boolean isConnected();

	boolean isServerReady();

	boolean awaitServerReady(long time, TimeUnit unit) throws InterruptedException;

}
