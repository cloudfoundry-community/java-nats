package jnats;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public final class Constants {

	/**
	 * The default host to look for a Nats server, localhost, naturally.
	 */
	public static final String DEFAULT_HOST = "localhost";
	/**
	 * The default Nats port, 4222.
	 */
	public static final int DEFAULT_PORT = 4222;
	/**
	 * The name of the Nats protocol to use in URIs.
	 */
	public static final String PROTOCOL = "nats";
	/**
	 * The default number of connection attempt to make for a particular Nats server before giving up.
	 */
	public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
	/**
	 * The default amount of time to wait between Nats server connection attempts.
	 */
	public static final long DEFAULT_RECONNECT_TIME_WAIT = TimeUnit.SECONDS.toMillis(2);

	/**
	 * The default maximum message size this client will accept from a Nats server.
	 */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 1048576;

	private Constants() {
		// Don't instantiate me.
	}

}
