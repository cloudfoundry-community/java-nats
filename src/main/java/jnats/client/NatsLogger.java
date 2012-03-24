package jnats.client;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public interface NatsLogger {
	public static enum Level {
		ERROR,
		WARNING,
		DEBUG
	}
	
	void log(Level level, String message);
	
	void log(Level level, Throwable t);
}
