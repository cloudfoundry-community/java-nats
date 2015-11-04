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

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath
 */
public final class Constants {

	/**
	 * The default host to look for a Nats server, localhost, naturally.
	 */
	public static final String DEFAULT_HOST = "localhost";
	/**
	 * The default time duration the connection to the NATS server may be idle.
	 */
	public static final long DEFAULT_IDLE_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
	/**
	 * The default time between Nats ping requests.
	 */
	public static final long DEFAULT_PING_INTERVAL = TimeUnit.SECONDS.toMillis(15);
	/**
	 * The default Nats port, 4222.
	 */
	public static final int DEFAULT_PORT = 4222;
	/**
	 * The name of the Nats protocol to use in URIs.
	 */
	public static final String PROTOCOL = "nats";
	/**
	 * The default amount of time to wait between Nats server connection attempts.
	 */
	public static final long DEFAULT_RECONNECT_TIME_WAIT = TimeUnit.SECONDS.toMillis(2);

	/**
	 * The default maximum message size this client will accept from a Nats server.
	 */
	public static final int DEFAULT_MAX_FRAME_SIZE = 1048576;

	private Constants() {
		// Don't instantiate me.
	}

}
