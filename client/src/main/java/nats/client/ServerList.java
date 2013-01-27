/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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

import nats.Constants;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps a list of available servers in the NATS cluster.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
class ServerList {

	public static int RECONNECT_LIMIT = 10;

	private final List<Server> servers = new ArrayList<>();

	private Server currentServer;

	private Iterator<Server> iterator;

	private final Object lock = new Object();

	public void addServer(URI uri) {
		final String host;
		final int port;
		if (uri.getHost() == null) {
			host = Constants.DEFAULT_HOST;
		} else {
			host = uri.getHost();
		}
		if (uri.getPort() > 0) {
			port = uri.getPort();
		} else {
			port = Constants.DEFAULT_PORT;
		}
		final SocketAddress address = new InetSocketAddress(host, port);
		String user = null;
		String password = null;
		if (uri.getUserInfo() != null) {
			final String userInfo = uri.getUserInfo();
			final String[] parts = userInfo.split(":");
			if (parts.length >= 1) {
				user = parts[0];
				if (parts.length >= 2) {
					password = parts[1];
				}
			}
		}
		synchronized (lock) {

			servers.add(new Server(address, user, password));
		}
	}

	public void addServers(Iterable<URI> servers) {
		for (URI uri : servers) {
			addServer(uri);
		}
	}

	public Server nextServer() {
		synchronized (lock) {
			if (servers.size() == 0)  {
				throw new IllegalStateException("No servers in list.");
			}

			if (iterator == null || !iterator.hasNext()) {
				final List<Server> activeServers = new ArrayList<>();
				for (Server server : servers) {
					if (server.connectionAttempts > 0) {
						activeServers.add(server);
					}
				}
				if (activeServers.size() == 0) {
					for (Server server : servers) {
						server.connectionAttempts = RECONNECT_LIMIT;
					}
					activeServers.addAll(servers);
				}
				Collections.shuffle(activeServers);
				iterator = activeServers.iterator();
			}
			currentServer = iterator.next();
			return currentServer;
		}
	}

	public Server getCurrentServer() {
		synchronized (lock) {
			return currentServer;
		}
	}

	public class Server {
		private final SocketAddress address;
		private final String user;
		private final String password;

		private int connectionAttempts = RECONNECT_LIMIT;

		private Server(SocketAddress address, String user, String password) {
			this.address = address;
			this.user = user;
			this.password = password;
		}

		public SocketAddress getAddress() {
			return address;
		}

		public String getUser() {
			return user;
		}

		public String getPassword() {
			return password;
		}

		public void connectionFailure() {
			synchronized (lock) {
				connectionAttempts--;
			}
		}

		public void connectionSuccess() {
			synchronized (lock) {
				connectionAttempts = RECONNECT_LIMIT;
			}
		}
	}

}
