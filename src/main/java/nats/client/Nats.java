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
import nats.NatsFuture;
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
import nats.codec.ServerErrorMessage;
import nats.codec.ServerInfoMessage;
import nats.codec.ServerOkMessage;
import nats.codec.ServerPongMessage;
import nats.codec.ServerPublishMessage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import java.io.Closeable;
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
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the interface for publishing messages and subscribing to NATS subjects. This class is responsible for
 * maintaining a connection to the NATS server as well as automatic fail-over to a second server if the
 * connection to one server fails.
 *
 * <p>This class is fully thread-safe.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Nats implements Closeable {

	/**
	 * The Netty {@link ChannelFactory} used for creating {@link Channel} objects for connecting to and communicating
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
	 * The current Netty {@link Channel} used for communicating with the Nats server. This field should never be null
	 * after the constructor has finished.
	 */
	private volatile Channel channel;

	/**
	 * The {@link Timer} used for scheduling server reconnects and scheduling delayed message publishing.
	 */
	private final Timer timer = new HashedWheelTimer();

	// Configuration values
	private final boolean automaticReconnect;
	private final int maxReconnectAttempts;
	private final long reconnectTimeWait;
	private final boolean pedantic;
	private final boolean verbose;

	private final ExceptionHandler exceptionHandler;
	private final NatsLogger logger;

	/**
	 * Indicates whether this {@code Nats} instance has been closed or not.
 	 */
	private volatile boolean closed = false;

	/**
	 * List of servers to try connecting to. This can be manually configured using
	 * {@link Builder#addHost(java.net.URI)} and gets updated based on server response to CONNECT message.
	 *
	 * <p>Must hold monitor #servers to access post creation.
	 */
	private final List<NatsServer> servers;

	/**
	 * Used for automatically rotating between available servers. Must hold monitor #servers to access.
	 */
	private Iterator<NatsServer> serverIterator;

	/**
	 * Holds the publish commands that have been queued up due to the connection being down.
	 *
	 * <p>Must hold monitor #publishQueue to access this queue.
	 */
	private final Queue<Publish> publishQueue = new LinkedList<Publish>();

	// Subscriptions
	/**
	 * Holds the list of subscriptions held by this {@code Nats} instance.
	 *
	 * <p>Must hold monitor #subscription to access.
	 */
	private final Map<Integer, NatsSubscription> subscriptions = new HashMap<Integer, NatsSubscription>();

	/**
	 * Counter used for obtaining subscription ids. Each subscription must have its own unique id that is sent to the
	 * NATS server to uniquely identify each subscription..
	 */
	private final AtomicInteger subscriptionId = new AtomicInteger();

	private final NatsConnectionStatus connectionStatus = new NatsConnectionStatus();

	/**
	 * Class used for configuring and creating {@link Nats} instances.
	 */
	public static class Builder {
		private List<URI> hosts = new ArrayList<URI>();
		private boolean automaticReconnect = true;
		private int maxReconnectAttempts = Constants.DEFAULT_MAX_RECONNECT_ATTEMPTS;
		private long reconnectWaitTime = Constants.DEFAULT_RECONNECT_TIME_WAIT;
		private boolean pedantic = false;
		private boolean verbose = false;
		private ChannelFactory channelFactory;
		private NatsLogger logger;
		private ExceptionHandler exceptionHandler;
		private int maxMessageSize = Constants.DEFAULT_MAX_MESSAGE_SIZE;

		/**
		 * Creates the {@code Nats} instance and asynchronously connects to the first Nats server provided using the
		 * {@code #addHost} methods. 
		 * 
		 * @return the {@code Nats} instance.
		 */
		public Nats connect() {
			if (hosts.size() == 0) {
				throw new IllegalStateException("No host specified to connect to.");
			}
			return new Nats(this);
		}

		/**
		 * Adds a URI to the list of URIs that will be used to connect to a Nats server by the {@link Nats} instance.
		 * 
		 * @param uri a Nats URI referencing a Nats server.
		 * @return this {@code Builder} instance.
		 */
		public Builder addHost(URI uri) {
			if (!Constants.PROTOCOL.equalsIgnoreCase(uri.getScheme())) {
				throw new IllegalArgumentException("Invalid protocol in URL: " + uri);
			}
			hosts.add(uri);
			return this;
		}

		/**
		 * Adds a URI to the list of URIs that will be used to connect to a Nats server by the {@link Nats} instance.
		 * 
		 * @param uri a Nats URI referencing a Nats server.
		 * @return this {@code Builder} instance.
		 */
		public Builder addHost(String uri) {
			return addHost(URI.create(uri));
		}

		/**
		 * Indicates whether a reconnect should be attempted automatically if the Nats server connection fails. Thsi
		 * value is {@code true} by default.
		 * 
		 * @param automaticReconnect whether a reconnect should be attempted automatically if the Nats server
		 *                           connection fails.
		 * @return this {@code Builder} instance.
		 */
		public Builder automaticReconnect(boolean automaticReconnect) {
			this.automaticReconnect = automaticReconnect;
			return this;
		}

		/**
		 * Specifies the Netty {@link ChannelFactory} to use for connecting to the Nats server(s). (optional)
		 * 
		 * @param channelFactory the Netty {@code ChannelFactory} to use for connecting to the Nats server(s)
		 * @return this {@code Builder} instance.
		 */
		public Builder channelFactory(ChannelFactory channelFactory) {
			this.channelFactory = channelFactory;
			return this;
		}

		/**
		 * Specifies the maximum number of subsequent connection attempts to make for a given server. (optional)
		 * 
		 * @param maxReconnectAttempts the maximum number of subsequent connection attempts to make for a given server
		 * @return this {@code Builder} instance.
		 */
		public Builder maxReconnectAttempts(int maxReconnectAttempts) {
			this.maxReconnectAttempts = maxReconnectAttempts;
			return this;
		}

		/**
		 * Specifies the amount of time to wait between connection attempts. This is only used when automatic
		 * reconnect is enabled.
		 * 
		 * @param time the amount of time to wait between connection attempts.
		 * @param unit the time unit of the {@code time} argument
		 * @return this {@code Builder} instance.
		 */
		public Builder reconnectWaitTime(long time, TimeUnit unit) {
			this.reconnectWaitTime = unit.toMillis(time);
			return this;
		}

		/**
		 * I have no idea what this is used for but both the Ruby and Node clients have it.
		 * 
		 * @param verbose
		 * @return this {@code Builder} instance.
		 */
		public Builder verbose(boolean verbose) {
			this.verbose = verbose;
			return this;
		}

		/**
		 * I have no idea what this is used for but both the Ruby and Node clients have it.
		 * 
		 * @param pedantic
		 * @return this {@code Builder} instance.
		 */
		public Builder pedantic(boolean pedantic) {
			this.pedantic = pedantic;
			return this;
		}

		/**
		 * Specifies the {@link NatsLogger} to be used by the {@code Nats} instance.
		 * 
		 * @param logger the {@code NatsLogger} to be used by the {@code Nats} instance.
		 * @return this {@code Builder} instance.
		 */
		public Builder logger(NatsLogger logger) {
			this.logger = logger;
			return this;
		}

		public Builder callback(ExceptionHandler exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
			return this;
		}

		/**
		 * Specified the maximum message size that can be received by the {@code} Nats instance. Defaults to 1 megabyte.
		 *
		 * @param maxMessageSize the maximum message size that can be received by the {@code} Nats instance.
		 * @return this {@code Builder} instance.
		 */
		public Builder maxMessageSize(int maxMessageSize) {
			this.maxMessageSize = maxMessageSize;
			return this;
		}
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

	private Nats(Builder builder) {
		// Create a default logger if one is not provided.
		if (builder.logger == null) {
			this.logger = NatsLogger.DEFAULT_LOGGER;
		} else {
			this.logger = builder.logger;
		}
		// Create a default exception handler if one is not provided
		if (builder.exceptionHandler == null) {
			this.exceptionHandler = new ExceptionHandler() {
				@Override
				public void onException(Throwable t) {
					logger.log(NatsLogger.Level.ERROR, t);
				}
			};
		} else {
			this.exceptionHandler = builder.exceptionHandler;
		}
		// Set up channel factory
		createdChannelFactory = builder.channelFactory == null;
		if (createdChannelFactory) {
			this.channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		} else {
			this.channelFactory = builder.channelFactory;
		}
		clientChannelPipelineFactory = new ClientChannelPipelineFactory(builder.maxMessageSize);

		// Setup server list
		this.servers = new ArrayList<NatsServer>();
		for (URI uri : builder.hosts) {
			this.servers.add(new NatsServer(uri));
		}

		// Set parameters
		automaticReconnect = builder.automaticReconnect;
		maxReconnectAttempts = builder.maxReconnectAttempts;
		reconnectTimeWait = builder.reconnectWaitTime;
		pedantic = builder.pedantic;
		verbose = builder.verbose;

		// Start connection to server
		connect();
	}

	private Channel createChannel(ChannelFactory channelFactory) {
		final ChannelPipeline pipeline = clientChannelPipelineFactory.getPipeline();
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
				// TODO Parse info body for alternative servers to connect to.
			}

			@Override
			public void okResponse(ChannelHandlerContext ctx, ClientRequest request, ServerOkMessage okMessage) {
				setRequestFutureDone(request, null);
				// Indicates we've successfully connected to a Nats server
				if (request instanceof ClientConnectMessage) {
					connectionStatus.setServerReady(true);
					// Resubscribe when the channel opens.
					synchronized (subscriptions) {
						for (NatsSubscription subscription : subscriptions.values()) {
							writeSubscription(subscription);
						}
					}
					// Resent pending publish commands.
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
				setRequestFutureDone(request, exception);
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
					setRequestFutureDone(request, closedException);
				}
			}

			@Override
			public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
				logger.log(NatsLogger.Level.DEBUG, "Connection closed");
				super.channelClosed(ctx, e);
				connectionStatus.channelClosed();
				if (automaticReconnect) {
					timer.newTimeout(new TimerTask() {
						@Override
						public void run(Timeout timeout) {
							connect();
						}
					}, reconnectTimeWait, TimeUnit.MILLISECONDS);
				}
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
				Throwable t = e.getCause();
				if (t instanceof TooLongFrameException) {
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

	private void setRequestFutureDone(ClientRequest request, Throwable cause) {
		if (request instanceof HasFuture) {
			((HasFuture)request).getFuture().setDone(cause);
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
					channel.write(new ClientConnectMessage(finalServer.user, finalServer.password, pedantic, verbose));
				} else {
					finalServer.incConnectionAttempts();
				}
			}
		});
	}

	public ConnectionStatus getConnectionStatus() {
		return connectionStatus;
	}

	/**
	 * Closes this Nats instance. Closes the connection to the Nats server, closes any subscriptions, cancels any
	 * pending messages to be published.
	 */
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
		NatsClosedException closedException = new NatsClosedException();
		final Set<Timeout> timeouts = timer.stop();
		for (Timeout timeout : timeouts) {
			final TimerTask task = timeout.getTask();
			if (task instanceof HasFuture) {
				((HasFuture)task).getFuture().setDone(closedException);
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
				publish.future.setDone(closedException);
			}
		}
	}

	/**
	 * Publishes an empty message to the specified subject. If this {@code Nats} instance is not currently connected to
	 * a Nats server, the message will be queued up to be published once a connection is established.
	 *
	 * @param subject the subject to publish to
	 * @return a {@code NatsFuture} object representing the pending publish.
	 */
	public PublishFuture publish(String subject) {
		return publish(subject, "", null);
	}

	/**
	 * Publishes a message to the specified subject. If this {@code Nats} instance is not currently connected to a Nats
	 * server, the message will be queued up to be published once a connection is established.
	 *
	 * @param subject the subject to publish to
	 * @param message the message to publish
	 * @return a {@code NatsFuture} object representing the pending publish.
	 */
	public PublishFuture publish(String subject, String message) {
		return publish(subject, message, null);
	}
	
	/**
	 * Publishes a message to the specified subject. If this {@code Nats} instance is not currently connected to a Nats
	 * server, the message will be queued up to be published once a connection is established.
	 *
	 * @param subject the subject to publish to
	 * @param message the message to publish
	 * @param replyTo the subject replies to this message should be sent to.
	 * @return a {@code NatsFuture} object representing the pending publish.
	 */
	public PublishFuture publish(String subject, String message, String replyTo) {
		assertNatsOpen();

		DefaultPublishFuture future = new DefaultPublishFuture(subject, message, replyTo, exceptionHandler);
		publish(subject, message, replyTo, future);
		return future;
	}

	private void publish(String subject, String message, String replyTo, DefaultPublishFuture future) {
		Publish publishMessage = new Publish(subject, message, replyTo, future);
		synchronized (publishQueue) {
			if (connectionStatus.isServerReady()) {
				channel.write(publishMessage);
			} else {
				publishQueue.add(publishMessage);
			}
		}
	}

	/**
	 * Subscribes to the specified subject.
	 *
	 * @see #subscribe(String, String, Integer)
	 * @param subject the subject to subscribe to.
	 * @return a {@code Subscription} object used for interacting with the subscription
	 */
	public Subscription subscribe(String subject) {
		return subscribe(subject, null, null);
	}
	
	/**
	 * Subscribes to the specified subject within a specific queue group. The subject can be a specific subject or
	 * include wild cards. A message to a particular subject will be delivered to only member of the same queue group.
	 * 
	 * @see #subscribe(String, String, Integer)
	 * @param subject the subject to subscribe to
	 * @param queueGroup the queue group the subscription participates in   
	 * @return a {@code Subscription} object used for interacting with the subscription
	 */
	public Subscription subscribe(String subject, String queueGroup) {
		return subscribe(subject, queueGroup, null);
	}

	/**
	 * Subscribes to the specified subject and will automatically unsubscribe after the specified number of messages
	 * arrives.
	 * 
	 * @see #subscribe(String, String, Integer)
	 * @param subject the subject to subscribe to
	 * @param maxMessages the number of messages this subscription will receive before automatically closing the
	 *                    subscription.
	 * @return a {@code Subscription} object used for interacting with the subscription
	 */
	public Subscription subscribe(String subject, Integer maxMessages) {
		return subscribe(subject, null, maxMessages);
	}

	/**
	 * Subscribes to the specified subject within a specific queue group and will automatically unsubscribe after the
	 * specified number of messages arrives.
	 * 
	 * <p>The {@code subject} may contain wild cards. "*" matches any token, at any level of the subject. For example:
	 * <pre>
	 *     "foo.*.baz"  matches "foo.bar.baz, foo.a.baz, etc.
	 *     "*.bar" matches "foo.bar", "baz.bar", etc.
	 *     "*.bar.*" matches "foo.bar.baz", "foo.bar.foo", etc.
	 * </pre>
	 *
	 * <p>">" matches any length of the tail of a subject and can only be the last token. For examples, 'foo.>' will
	 * match 'foo.bar', 'foo.bar.baz', 'foo.foo.bar.bax.22'. A subject of simply ">" will match all messages.
	 *
	 * <p>All subscriptions with the same {@code queueGroup} will form a queue group. Each message will be delivered to
	 * only one subscriber per queue group.
	 *
	 * @param subject the subject to subscribe to
	 * @param queueGroup the queue group the subscription participates in   
	 * @param maxMessages the number of messages this subscription will receive before automatically closing the
	 *                    subscription.
	 * @return a {@code Subscription} object used for interacting with the subscription
	 */
	public Subscription subscribe(final String subject, final String queueGroup, final Integer maxMessages) {
		assertNatsOpen();
		final Integer id = subscriptionId.incrementAndGet();
		NatsSubscription subscription = new NatsSubscription() {
			private final AtomicInteger receivedMessages = new AtomicInteger();
			private final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
			private final List<BlockingQueueSubscriptionIterator> iterators = new ArrayList<BlockingQueueSubscriptionIterator>();
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

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public HandlerRegistration addMessageHandler(final MessageHandler messageHandler) {
				synchronized (handlers) {
					handlers.add(messageHandler);
				}
				return new HandlerRegistration() {
					@Override
					public void remove() {
						synchronized (handlers) {
							handlers.remove(messageHandler);
						}
					}
				};
			}

			@Override
			public SubscriptionIterator iterator() {
				final BlockingQueueSubscriptionIterator iterator = new BlockingQueueSubscriptionIterator();
				synchronized (iterators)  {
					iterators.add(iterator);
				}
				return iterator;
			}

			@Override
			public int getReceivedMessages() {
				return receivedMessages.get();
			}

			@Override
			public Integer getMaxMessages() {
				return maxMessages;
			}

			@Override
			public String getQueueGroup() {
				return queueGroup;
			}
			@Override
			public void onMessage(final String subject, final String body, final String replyTo) {
				final int messageCount = receivedMessages.incrementAndGet();
				if (maxMessages != null && messageCount >= maxMessages) {
					close();
				}
				final Subscription subscription = this;
				final boolean hasReply = replyTo != null && replyTo.trim().length() > 0;
				Message message = new Message() {
					@Override
					public Subscription getSubscription() {
						return subscription;
					}

					@Override
					public String getSubject() {
						return subject;
					}

					@Override
					public String getBody() {
						return body;
					}

					@Override
					public String getReplyTo() {
						return replyTo;
					}

					@Override
					public NatsFuture reply(String message) {
						if (!hasReply) {
							throw new NatsException("Message does not have a replyTo address to send the message to.");
						}
						return publish(replyTo, message);

					}

					@Override
					public PublishFuture reply(final String message, long delay, TimeUnit unit) {
						if (!hasReply) {
							throw new NatsException("Message does not have a replyTo address to send the message to.");
						}
						final DefaultPublishFuture future = new DefaultPublishFuture(replyTo, message, null, exceptionHandler);
						timer.newTimeout(new TimerTaskFuture() {
							@Override
							public void run(Timeout timeout) {
								publish(replyTo, message, null, future);
							}

							@Override
							public DefaultPublishFuture getFuture() {
								return future;
							}
						}, delay, unit);
						return future;
					}

					@Override
					public String toString() {
						StringBuilder builder = new StringBuilder();
						builder.append("[subject: '").append(subject).append("', body: '").append(body).append("'");
						if (hasReply) {
							builder.append(", replyTo: '").append(replyTo).append("'");
						}
						builder.append(']');
						return builder.toString();
					}
				};
				synchronized (handlers) {
					for (MessageHandler handler : handlers) {
						try {
							handler.onMessage(message);
						} catch (Throwable t) {
							exceptionHandler.onException(t);
						}
					}
				}
				synchronized (iterators) {
					for (BlockingQueueSubscriptionIterator iterator : iterators) {
						try {
							iterator.push(message);
						} catch (Throwable t) {
							exceptionHandler.onException(t);
						}
					}
				}
			}

			@Override
			public SubscriptionTimeout timeout(long time, TimeUnit unit) {
				return new DefaultSubscriptionTimeout(timer, this, exceptionHandler, time, unit);
			}

			@Override
			public Integer getId() {
				return id;
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

	/**
	 * Sends a request on the given subject with an empty message. Request responses can be handled using the returned
	 * {@link RequestFuture}.
	 *
	 * @see #request(String, String, Integer)
	 * @param subject the subject to send the request on
	 * @return
	 */
	public RequestFuture request(String subject) {
		return request(subject, "", null);
	}

	/**
	 * Sends a request message on the given subject. Request responses can be handled using the returned
	 * {@link RequestFuture}.
	 * 
	 * @see #request(String, String, Integer)
	 * @param subject the subject to send the request on
	 * @param message the content of the request
	 * @return
	 */
	public RequestFuture request(String subject, String message) {
		return request(subject, message, null);
	}

	/**
	 * Sends a request message on the given subject. Request responses can be handled using the returned
	 * {@link RequestFuture}.
	 *
	 * Invoking this method is roughly equivalent to the following:
	 *
	 * <code>
	 *     String replyTo = Nats.createInbox();
	 *     Subscription subscription = nats.subscribe(replyTo, maxReplies);
	 *     NatsFuture publishFuture = nats.publish(subject, message, replyTo);
	 * </code>
	 *
	 * that returns a combination of {@code subscription} and {@code natsFuture} as a {@code RequestFuture} object.
	 *
	 * @param subject the subject to send the request on
	 * @param message the content of the request
	 * @param maxReplies the maximum number of replies that the request will accept before automatically closing,
	 *                   {@code null} for unlimited replies
	 * @return
	 */
	public RequestFuture request(String subject, final String message, final Integer maxReplies) {
		assertNatsOpen();
		final String inbox = createInbox();
		final Subscription subscription = subscribe(inbox, maxReplies);
		final PublishFuture publishFuture = publish(subject, message, inbox);
		return new DefaultRequestFuture(subscription, publishFuture);
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
	
	private static interface NatsSubscription extends Subscription {
		void onMessage(String subject, String message, String replyTo);
		Integer getId();
	}

	private static interface HasFuture {
		DefaultPublishFuture getFuture();
	}

	private static interface TimerTaskFuture extends TimerTask, HasFuture {}

	private static class Publish extends ClientPublishMessage implements HasFuture {
		private final DefaultPublishFuture future;

		public Publish(String subject, String message, String replyTo, DefaultPublishFuture future) {
			super(subject, message, replyTo);
			this.future = future;
		}

		@Override
		public DefaultPublishFuture getFuture() {
			return future;
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
