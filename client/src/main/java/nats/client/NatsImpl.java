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

import nats.Constants;
import nats.HandlerRegistration;
import nats.NatsException;
import nats.NatsLogger;
import nats.NatsServerException;
import nats.codec.AbstractClientChannelHandler;
import nats.codec.ClientChannelPipelineFactory;
import nats.codec.ClientConnectMessage;
import nats.codec.ClientPingMessage;
import nats.codec.ClientPublishMessage;
import nats.codec.ClientRequest;
import nats.codec.ClientSubscribeMessage;
import nats.codec.ClientUnsubscribeMessage;
import nats.codec.ConnectBody;
import nats.codec.NatsDecodingException;
import nats.codec.ServerErrorMessage;
import nats.codec.ServerInfoMessage;
import nats.codec.ServerOkMessage;
import nats.codec.ServerPongMessage;
import nats.codec.ServerPublishMessage;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class NatsImpl implements Nats {

	/**
	 * The Netty {@link org.jboss.netty.channel.ChannelFactory} used for creating {@link org.jboss.netty.channel.Channel} objects for connecting to and communicating
	 * with Nats servers.
	 */
	private final ChannelFactory channelFactory;
	/**
	 * Indicates whether this class created the {@link ChannelFactory}. If this field is false, the
	 * {@code ChannelFactory} was provided by the user of this class and the channel factory resources will not be
	 * released when {@link #close()} is invoked.
	 */
	private final boolean createdChannelFactory;

	private final ClientChannelPipelineFactory clientChannelPipelineFactory;
	/**
	 * The current Netty {@link org.jboss.netty.channel.Channel} used for communicating with the Nats server. This field should never be null
	 * after the constructor has finished.
	 */
	private volatile Channel channel;

	/**
	 * The {@link org.jboss.netty.util.Timer} used for scheduling server reconnects and scheduling delayed message publishing.
	 */
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	// Configuration values
	private final boolean automaticReconnect;
	private final int maxReconnectAttempts;
	private final long reconnectTimeWait;
	private final boolean pedantic;

	private final ExceptionHandler exceptionHandler;
	private final NatsLogger logger;

	/**
	 * Indicates whether this {@code Nats} instance has been closed or not.
	 */
	private volatile boolean closed = false;

	/**
	 * List of servers to try connecting to. This can be manually configured using
	 * {@link NatsConnector#addHost(java.net.URI)} and gets updated based on server response to CONNECT message.
	 * <p/>
	 * <p>Must hold monitor #servers to access post creation.
	 */
	private final List<NatsServer> servers;

	/**
	 * Used for automatically rotating between available servers. Must hold monitor #servers to access.
	 */
	private Iterator<NatsServer> serverIterator;

	/**
	 * Holds the publish commands that have been queued up due to the connection being down.
	 * <p/>
	 * <p>Must hold monitor #publishQueue to access this queue.
	 */
	private final Queue<Publish> publishQueue = new LinkedList<Publish>();

	// Subscriptions
	/**
	 * Holds the list of subscriptions held by this {@code Nats} instance.
	 * <p/>
	 * <p>Must hold monitor #subscription to access.
	 */
	private final Map<String, NatsSubscription> subscriptions = new HashMap<String, NatsSubscription>();

	/**
	 * Counter used for obtaining subscription ids. Each subscription must have its own unique id that is sent to the
	 * NATS server to uniquely identify each subscription..
	 */
	private final AtomicInteger subscriptionId = new AtomicInteger();

	private final NatsConnectionStatus connectionStatus = new NatsConnectionStatus();

	private final boolean debug;

	/**
	 * Class used for configuring and creating {@link Nats} instances.
	 */
	public static class Builder {
	}

	private static final Random random = new Random();

	/**
	 * Generates a random string used for creating a unique string. The {@code request} methods rely on this
	 * functionality.
	 *
	 * @return a unique random string.
	 */
	public static String createInbox() {
		byte[] bytes = new byte[16];
		synchronized (random) {
			random.nextBytes(bytes);
		}
		return "_INBOX." + new BigInteger(bytes).abs().toString(16);
	}

	NatsImpl(NatsConnector connector) {
		debug = connector.debug;
		// Create a default logger if one is not provided.
		if (connector.logger == null) {
			if (debug) {
				this.logger = NatsLogger.DEBUG_LOGGER;
			} else {
				this.logger = NatsLogger.DEFAULT_LOGGER;
			}
		} else {
			this.logger = connector.logger;
		}
		// Create a default exception handler if one is not provided
		if (connector.exceptionHandler == null) {
			this.exceptionHandler = new ExceptionHandler() {
				@Override
				public void onException(Throwable t) {
					logger.log(NatsLogger.Level.ERROR, t);
				}
			};
		} else {
			this.exceptionHandler = connector.exceptionHandler;
		}
		// Set up channel factory
		createdChannelFactory = connector.channelFactory == null;
		if (createdChannelFactory) {
			this.channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		} else {
			this.channelFactory = connector.channelFactory;
		}
		clientChannelPipelineFactory = new ClientChannelPipelineFactory(connector.maxMessageSize);

		// Setup server list
		this.servers = new ArrayList<NatsServer>();
		for (URI uri : connector.hosts) {
			this.servers.add(new NatsServer(uri));
		}

		// Set parameters
		automaticReconnect = connector.automaticReconnect;
		maxReconnectAttempts = connector.maxReconnectAttempts;
		reconnectTimeWait = connector.reconnectWaitTime;
		pedantic = connector.pedantic;

		// Start connection to server
		connect();
	}

	private Channel createChannel(ChannelFactory channelFactory) {
		final ChannelPipeline pipeline = clientChannelPipelineFactory.getPipeline();
		if (debug) {
			pipeline.addFirst("debugger", new SimpleChannelHandler() {
				@Override
				public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
					if (e instanceof MessageEvent) {
						final ChannelBuffer message = (ChannelBuffer) ((MessageEvent) e).getMessage();
						System.out.println("OUTGOING: " + new String(message.array()));
					}
					super.handleDownstream(ctx, e);
				}

				@Override
				public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
					if (e instanceof MessageEvent) {
						final ChannelBuffer message = (ChannelBuffer) ((MessageEvent) e).getMessage();
						System.out.println("INCOMING: " + new String(message.array()));
					}
					super.handleUpstream(ctx, e);
				}
			});
		}
		pipeline.addLast("handler", new AbstractClientChannelHandler() {
			@Override
			public void publishedMessage(ChannelHandlerContext ctx, ServerPublishMessage message) {
				final NatsSubscription subscription;
				synchronized (subscriptions) {
					subscription = subscriptions.get(message.getId());
				}
				if (subscription == null) {
					throw new NatsException("Received a message for an unknown subscription.");
				}
				subscription.onMessage(message.getSubject(), message.getBody(), message.getReplyTo());
			}

			@Override
			public void pongResponse(ChannelHandlerContext ctx, ClientPingMessage pingMessage, ServerPongMessage pongMessage) {
				// Do nothing, we don't do any pings to respond to a pong.
			}

			@Override
			public void serverInfo(ChannelHandlerContext ctx, ServerInfoMessage infoMessage) {
				// TODO Parse info body for alternative servers to connect to as soon as NATS' clustering support starts sending this.
			}

			@Override
			public void okResponse(ChannelHandlerContext ctx, ClientRequest request, ServerOkMessage okMessage) {
				completePublication(request, null);
				// Indicates we've successfully connected to a Nats server
				if (request instanceof ClientConnectMessage) {
					connectionStatus.setServerReady(true);
					// Resubscribe when the channel opens.
					synchronized (subscriptions) {
						for (NatsSubscription subscription : subscriptions.values()) {
							writeSubscription(subscription);
						}
					}
					// Resend pending publish commands.
					synchronized (publishQueue) {
						for (Publish publish : publishQueue) {
							ctx.getChannel().write(publish);
						}
					}
				}
			}

			@Override
			public void errorResponse(ChannelHandlerContext ctx, ClientRequest request, ServerErrorMessage errorMessage) {
				final StringBuilder message = new StringBuilder().append("Received error response from server");
				final String errorText = errorMessage.getErrorMessage();
				if (errorText != null && errorText.trim().length() > 0) {
					message.append(": ").append(errorText);
				}
				final NatsServerException exception = new NatsServerException(message.toString());
				completePublication(request, exception);
				throw exception;
			}

			@Override
			protected void handleOrphanedPings(ChannelHandlerContext ctx, Collection<ClientPingMessage> pings) {
				// Do nothing, we don't do any pinging.
			}

			@Override
			protected void handleOrphanedRequests(ChannelHandlerContext ctx, Collection<ClientRequest> requests) {
				final NatsClosedException closedException = new NatsClosedException();
				for (ClientRequest request : requests) {
					completePublication(request, closedException);
				}
			}

			@Override
			public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
				logger.log(NatsLogger.Level.DEBUG, "Connection closed");
				super.channelClosed(ctx, e);
				connectionStatus.channelClosed();
				if (automaticReconnect) {
					scheduledExecutorService.schedule(new Runnable() {
						@Override
						public void run() {
							connect();
						}
					}, reconnectTimeWait, TimeUnit.MILLISECONDS);
				}
			}

			@Override
			@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
			public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
				Throwable t = e.getCause();
				if (t instanceof TooLongFrameException || t instanceof NatsDecodingException) {
					logger.log(NatsLogger.Level.ERROR, "Closing due to: " + t);
					close();
				}
				exceptionHandler.onException(e.getCause());
			}

			@Override
			public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
				super.writeRequested(ctx, e);
			}
		});
		return channelFactory.newChannel(pipeline);
	}

	private void completePublication(ClientRequest request, Throwable cause) {
		if (request instanceof HasPublication) {
			((HasPublication) request).getPublication().setDone(cause);
		}
	}

	private void connect() {
		NatsServer server = null;
		synchronized (servers) {
			while (server == null) {
				if (serverIterator == null || !serverIterator.hasNext()) {
					if (servers.size() == 0) {
						exceptionHandler.onException(new ServerConnectionFailedException("Unable to connect a NATS server."));
						close();
						throw new NatsEmptyServerListException();
					}
					serverIterator = servers.iterator();
				}
				server = serverIterator.next();
				if (maxReconnectAttempts > 0 && server.getConnectionAttempts() > maxReconnectAttempts) {
					logger.log(NatsLogger.Level.WARNING, "Exceeded max connection attempts connecting to Nats server " + server.address);
					exceptionHandler.onException(new ServerReconnectFailed(server.address));
					serverIterator.remove();
				}
			}
		}
		final NatsServer finalServer = server;
		channel = createChannel(channelFactory);
		channel.connect(server.address).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				// If connection is successful, set connection attempts to 0, otherwise increase connection attempts.
				if (future.isSuccess()) {
					finalServer.resetConnectionAttempts();
					channel.write(new ClientConnectMessage(new ConnectBody(finalServer.user, finalServer.password, pedantic, true)));
				} else {
					finalServer.incConnectionAttempts();
				}
			}
		});
	}

	@Override
	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		channel.close().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				cleanupResources();
			}
		});
	}

	private void cleanupResources() {
		if (createdChannelFactory) {
			channelFactory.releaseExternalResources();
		}
		final NatsClosedException closedException = new NatsClosedException();
		final List<Runnable> pendingTasks = scheduledExecutorService.shutdownNow();
		for (Runnable task : pendingTasks) {
			if (task instanceof HasPublication) {
				((HasPublication) task).getPublication().setDone(closedException);
			}
		}
		synchronized (subscriptions) {
			// We have to make a copy of the subscriptions because calling subscription.close() modifies the subscriptions map.
			Collection<Subscription> subscriptionsCopy = new ArrayList<Subscription>(subscriptions.values());
			for (Subscription subscription : subscriptionsCopy) {
				subscription.close();
			}
		}
		synchronized (publishQueue) {
			for (Publish publish : publishQueue) {
				publish.publication.setDone(closedException);
			}
		}
	}

	@Override
	public Publication publish(String subject) {
		return publish(subject, "", null);
	}

	@Override
	public Publication publish(String subject, String message) {
		return publish(subject, message, null);
	}

	@Override
	public Publication publish(String subject, String message, String replyTo) {
		assertNatsOpen();

		DefaultPublication publication = new DefaultPublication(subject, message, replyTo, exceptionHandler);
		publish(publication);
		return publication;
	}

	private void publish(DefaultPublication publication) {
		Publish publishMessage = new Publish(publication);
		synchronized (publishQueue) {
			if (connectionStatus.isServerReady()) {
				channel.write(publishMessage);
			} else {
				publishQueue.add(publishMessage);
			}
		}
	}

	@Override
	public Subscription subscribe(String subject) {
		return subscribe(subject, null, null);
	}

	@Override
	public Subscription subscribe(String subject, String queueGroup) {
		return subscribe(subject, queueGroup, null);
	}

	@Override
	public Subscription subscribe(String subject, Integer maxMessages) {
		return subscribe(subject, null, maxMessages);
	}

	@Override
	public Subscription subscribe(final String subject, final String queueGroup, final Integer maxMessages) {
		assertNatsOpen();
		final String id = Integer.toString(subscriptionId.incrementAndGet());
		NatsSubscription subscription = new NatsSubscription() {
			@Override
			public void close() {
				synchronized (subscriptions) {
					subscriptions.remove(id);
				}
				synchronized (iterators) {
					for (BlockingQueueSubscriptionIterator iterator : iterators) {
						iterator.close();
					}
				}
				if (maxMessages == null) {
					if (!closed) {
						channel.write(new ClientUnsubscribeMessage(id, maxMessages));
					}
				}
			}
		};

		synchronized (subscriptions) {
			subscriptions.put(id, subscription);
		}
		writeSubscription(subscription);
		return subscription;
	}

	private void writeSubscription(NatsSubscription subscription) {
		if (connectionStatus.isServerReady()) {
			channel.write(new ClientSubscribeMessage(subscription.getId(), subscription.getSubject(), subscription.getQueueGroup()));
		}
	}

	@Override
	public Request request(String subject, MessageHandler... messageHandlers) {
		return request(subject, "", null, messageHandlers);
	}

	@Override
	public Request request(String subject, String message, MessageHandler... messageHandlers) {
		return request(subject, message, null, messageHandlers);
	}

	@Override
	public Request request(String subject, String message, Integer maxReplies, MessageHandler... messageHandlers) {
		assertNatsOpen();
		final String inbox = createInbox();
		final Subscription subscription = subscribe(inbox, maxReplies);
		for (MessageHandler handler : messageHandlers) {
			subscription.addMessageHandler(handler);
		}
		final DefaultPublication publication = new DefaultPublication(subject, message, inbox, exceptionHandler);
		publish(publication);
		return new DefaultRequest(subscription, publication);
	}

	private void assertNatsOpen() {
		if (closed) {
			throw new NatsClosedException();
		}
	}

	private static class NatsServer {
		private final SocketAddress address;
		private final String user;
		private final String password;

		/**
		 * Access must be synchronized on NatsServer instance.
		 */
		private int connectionAttempts = 0;

		public NatsServer(URI uri) {
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
			this.address = new InetSocketAddress(host, port);
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
			this.user = user;
			this.password = password;
		}

		public long getConnectionAttempts() {
			synchronized (this) {
				return connectionAttempts;
			}
		}

		public int incConnectionAttempts() {
			synchronized (this) {
				return ++connectionAttempts;
			}
		}

		public void resetConnectionAttempts() {
			synchronized (this) {
				connectionAttempts = 0;
			}
		}

	}

	private abstract static class NatsSubscription extends AbstractSubscription {
		void onMessage(String subject, String message, String replyTo);

		String getId();
	}

	private static interface HasPublication {
		DefaultPublication getPublication();
	}

	private static interface ScheduledPublication extends Runnable, HasPublication {
	}

	private static class Publish extends ClientPublishMessage implements HasPublication {
		private final DefaultPublication publication;

		public Publish(DefaultPublication publication) {
			super(publication.getSubject(), publication.getMessage(), publication.getReplyTo());
			this.publication = publication;
		}

		@Override
		public DefaultPublication getPublication() {
			return publication;
		}
	}

	private class NatsConnectionStatus implements ConnectionStatus {

		private volatile boolean serverReady = false;
		private final Object serverReadyMonitor = new Object();
		private final Object connectionClosedMonitor = new Object();

		@Override
		public boolean isConnected() {
			return channel.isConnected();
		}

		@Override
		public boolean isServerReady() {
			return serverReady;
		}

		@Override
		public boolean awaitConnectionClose(long time, TimeUnit unit) throws InterruptedException {
			if (channel.isConnected()) {
				synchronized (connectionClosedMonitor) {
					connectionClosedMonitor.wait(unit.toMillis(time));
				}
			}
			return !channel.isConnected();
		}

		@Override
		public boolean awaitServerReady(long time, TimeUnit unit) throws InterruptedException {
			synchronized (serverReadyMonitor) {
				serverReadyMonitor.wait(unit.toMillis(time));
			}
			return serverReady;
		}

		@Override
		public SocketAddress getLocalAddress() {
			return channel.getLocalAddress();
		}

		@Override
		public SocketAddress getRemoteAddress() {
			return channel.getRemoteAddress();
		}

		public void setServerReady(boolean ready) {
			this.serverReady = ready;
			if (serverReady) {
				synchronized (serverReadyMonitor) {
					serverReadyMonitor.notifyAll();
				}
			}
		}

		public void channelClosed() {
			setServerReady(false);
			synchronized (connectionClosedMonitor) {
				connectionClosedMonitor.notifyAll();
			}
		}
	}
}
