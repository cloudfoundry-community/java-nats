package jnats;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public interface NatsLogger {
	public static enum Level {
		ERROR,
		WARNING,
		DEBUG
	}

	public static final NatsLogger DEFAULT_LOGGER = new NatsLogger() {
		@Override
		public void log(Level level, String message) {
			if (level != Level.DEBUG) {
				System.out.println(message);
			}
		}

		@Override
		public void log(Level level, Throwable t) {
			if (level != Level.DEBUG) {
				t.printStackTrace();
			}
		}
	};

	void log(Level level, String message);
	
	void log(Level level, Throwable t);
}
