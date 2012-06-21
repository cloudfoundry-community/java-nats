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
package nats;

/**
 * @author Mike Heath <elcapo@gmail.com>
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

	public static final NatsLogger DEBUG_LOGGER = new NatsLogger() {
		@Override
		public void log(Level level, String message) {
			System.out.println(level.toString() + ": " + message);
		}

		@Override
		public void log(Level level, Throwable t) {
			t.printStackTrace();
		}
	};

	void log(Level level, String message);
	
	void log(Level level, Throwable t);
}
