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
package jnats.client;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.io.Closeable;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Blocking mode for subscription
// TODO Optional delay parameter for publish
// TODO Add ping support that returns a future and the future completes when the pong arrives - useful for testing.
// TODO Write a bunch of integration tests (automatic failover, publish, subscribe, subscribe max, publish future stuff when fail over happens, max reconnect attempts)
// TODO Java Docs
// TODO Write a markdown documentation page for Git Hub site
// TODO Publish to GitHub
// TODO Tweet about it!
public class Nats implements Closeable {

	// Default configuration values.
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 4222;
	public static final String PROTOCOL = "nats";
	public static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
	public static final long DEFAULT_RECONNECT_TIME_WAIT = TimeUnit.SECONDS.toMillis(2);

	public static final int MAX_MESSAGE_SIZE = 1048576;

	private final ChannelFactory channelFactory;
	private final boolean createChannelFactory;
	private volatile Channel channel;

	private final Timer timer;

	// Configuration values
	private final int maxReconnectAttempts;
	private final long reconnectTimeWait;
	private final boolean pedantic;
	private final boolean verbose;

	private final Callback callback;
	private final NatsLogger logger;

	// Indicates whether the Nats instance has been closed or not.
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

	/**
	 * Holds the future objects associated with each #ping() request.
	 * 
	 * <p>The pongQueue may only be accessed from the Netty IO thread.
	 */
	private final Queue<NatsFutureImpl> pongQueue = new LinkedList<NatsFutureImpl>();

	// Subscriptions
	/**
	 * Holds the list of subscriptions held by this Nats instance. 
	 *
	 * <p>Must hold monitor #subscription to access.
	 */
	private final Map<Integer, NatsSubscription> subscriptions = new HashMap<Integer, NatsSubscription>();
	private final AtomicInteger subscriptionId = new AtomicInteger();

	// Constants for over wire protocol
	private static final String CMD_CONNECT = "CONNECT";
	private static final String CMD_PING = "PING\r\n";
	private static final String CMD_PONG = "PONG\r\n";
	private static final String CMD_PUBLISH = "PUB";
	private static final String CMD_SUBSCRIBE = "SUB";
	private static final String CMD_UNSUBSCRIBE = "UNSUB";

	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s+('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	public static class Builder {
		private List<URI> hosts = new ArrayList<URI>();
		private boolean automaticReconnect = true;
		private int maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS;
		private long reconnectWaitTime = DEFAULT_RECONNECT_TIME_WAIT;
		private boolean pedantic = false;
		private boolean verbose = false;
		private ChannelFactory channelFactory;
		private NatsLogger logger;
		private Callback callback;

		public Nats connect() {
			if (hosts.size() == 0) {
				throw new IllegalStateException("No host specified to connect to.");
			}
			return new Nats(this);
		}
		
		public Builder addHost(URI uri) {
			if (!PROTOCOL.equalsIgnoreCase(uri.getScheme())) {
				throw new IllegalArgumentException("Invalid protocol in URL: " + uri);
			}
			hosts.add(uri);
			return this;
		}
		
		public Builder addHost(String host) {
			return addHost(URI.create(host));
		}

		public Builder automaticReconnect(boolean automaticReconnect) {
			this.automaticReconnect = automaticReconnect;
			return this;
		}
		
		public Builder channelFactory(ChannelFactory channelFactory) {
			this.channelFactory = channelFactory;
			return this;
		}
		
		public Builder maxReconnectAttempts(int maxReconnectAttempts) {
			this.maxReconnectAttempts = maxReconnectAttempts;
			return this;
		}
		
		public Builder reconnectWaitTime(long reconnectWaitTime) {
			this.reconnectWaitTime = reconnectWaitTime;
			return this;
		}

		public Builder verbose(boolean verbose) {
			this.verbose = verbose;
			return this;
		}

		public Builder pedantic(boolean pedantic) {
			this.pedantic = pedantic;
			return this;
		}
		
		public Builder logger(NatsLogger logger) {
			this.logger = logger;
			return this;
		}

		public Builder callback(Callback callback) {
			this.callback = callback;
			return this;
		}

	}

	private static final StringDecoder decoder = new StringDecoder();
	private static final StringEncoder encoder = new StringEncoder();

	private static final String PIPELINE_FRAME_DECODER = "frameDecoder";
	private static final String PIPELINE_STRING_DECODER = "stringDecoder";
	private static final String PIPELINE_STRING_ENCODER = "stringEncoder";
	private static final String PIPELINE_FIXED_DECODER = "fixedDecoder";
	private static final String PIPELINE_HANDLER = "handler";

	private static final Random random = new Random();

	public static String createInbox() {
		byte[] bytes = new byte[16];
		synchronized (random) {
			random.nextBytes(bytes);
		}
		return "_INBOX." + new BigInteger(bytes).abs().toString(16);
	}

	private Nats(Builder builder) {
		if (builder.logger == null) {
			this.logger = new NatsLogger() {

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
		} else {
			this.logger = builder.logger;
		}
		if (builder.callback == null) {
			this.callback = new Callback() {
				@Override
				public void onConnect() {
					logger.log(NatsLogger.Level.DEBUG, "Connect to server");
				}

				@Override
				public void onClose() {
					logger.log(NatsLogger.Level.WARNING, "Connection closed");
				}

				@Override
				public void onException(Throwable t) {
					logger.log(NatsLogger.Level.ERROR, t);
				}

				@Override
				public void onServerReconnectFailed(SocketAddress address) {
					logger.log(NatsLogger.Level.ERROR, "Unable to connect to server " + address);
				}

				@Override
				public void onServerConnectFailed() {
					logger.log(NatsLogger.Level.ERROR, "Unable to connect to any of the provided Nats servers.");
				}
			};
		} else {
			this.callback = builder.callback;
		}
		createChannelFactory = builder.channelFactory == null;
		if (createChannelFactory) {
			this.channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		} else {
			this.channelFactory = builder.channelFactory;
		}
		this.servers = new ArrayList<NatsServer>();
		for (URI uri : builder.hosts) {
			this.servers.add(new NatsServer(uri));
		}
		if (builder.automaticReconnect) {
			timer = new Timer("nats");
		} else {
			timer = null;
		}
		maxReconnectAttempts = builder.maxReconnectAttempts;
		reconnectTimeWait = builder.reconnectWaitTime;
		pedantic = builder.pedantic;
		verbose = builder.verbose;
		connect();
	}

	private Channel createChannel(ChannelFactory channelFactory) {
		final ChannelPipeline pipeline = Channels.pipeline();
		final DelimiterBasedFrameDecoder delimiterBasedFrameDecoder = new DelimiterBasedFrameDecoder(MAX_MESSAGE_SIZE, ChannelBuffers.wrappedBuffer(new byte[]{'\r', '\n'}));
		pipeline.addFirst(PIPELINE_FRAME_DECODER, delimiterBasedFrameDecoder);
		pipeline.addLast(PIPELINE_STRING_DECODER, decoder);
		pipeline.addLast(PIPELINE_STRING_ENCODER, encoder);
		pipeline.addLast(PIPELINE_HANDLER, new SimpleChannelUpstreamHandler() {
			private NatsSubscription currentSubscription;
			private String replyTo;
			@Override
			public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
				final Object message = e.getMessage();
				if (message instanceof String) {
					String payload = (String) message;
					logger.log(NatsLogger.Level.DEBUG, "Server message: " + payload);
					final Matcher matcher = MSG_PATTERN.matcher(payload);
					if (currentSubscription != null) {
						try {
							currentSubscription.onMessage(payload, replyTo);
						} finally {
							currentSubscription = null;
							replyTo = null;
							ctx.getPipeline().replace(PIPELINE_FIXED_DECODER, PIPELINE_FRAME_DECODER, delimiterBasedFrameDecoder);
						}
					} else if (matcher.matches()) {
						final int id = Integer.valueOf(matcher.group(2));
						final int length = Integer.valueOf(matcher.group(5));
						currentSubscription = subscriptions.get(id);
						if (currentSubscription != null) {
							this.replyTo = matcher.group(4);
							ctx.getPipeline().replace(PIPELINE_FRAME_DECODER, PIPELINE_FIXED_DECODER, new FixedLengthFrameDecoder(length));
						}
					} else if (INFO_PATTERN.matcher(payload).matches()) {
						// TODO Parse info body for alternative servers to connect to.
					} else if (OK_PATTERN.matcher(payload).matches()) {
						// Just ignore
					} else if ((ERR_PATTERN.matcher(payload)).matches()) {
						throw new NatsServerException(payload);
					} else if (PING_PATTERN.matcher(payload).matches()) {
						ctx.getChannel().write(CMD_PONG);
					} else if (PONG_PATTERN.matcher(payload).matches()) {
						final NatsFutureImpl pongFuture = pongQueue.poll();
						if (pongFuture == null) {
							throw new NatsException("Received unexpected PONG from server.");
						} else {
							pongFuture.setDone(null);
						}
					} else {
						throw new NatsServerException("Don't know how to handle the following sent by the Nats server: " + payload);
					}
				} else {
					ctx.sendUpstream(e);
				}
			}

			@Override
			public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
				if (timer != null) {
					timer.schedule(new TimerTask() {
						@Override
						public void run() {
							connect();
						}
					}, reconnectTimeWait);
				}
				final NatsClosedException closedException = new NatsClosedException();
				for (NatsFutureImpl future : pongQueue) {
					future.setDone(closedException);
				}
				callback.onClose();
			}

			@Override
			public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
				callback.onConnect();
				// Resubscribe when the channel opens.
				synchronized (subscriptions) {
					for (NatsSubscription subscription:subscriptions.values()){
						writeSubscription(subscription);
					}
				}
				// Resent pending publish commands.
				synchronized (publishQueue) {
					for (Publish publish : publishQueue) {
						writePublishCommand(publish.publishCommand, publish.future);
					}
				}
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
				callback.onException(e.getCause());
			}
		});
		return channelFactory.newChannel(pipeline);
	}

	private void connect() {
		NatsServer server = null;
		synchronized (servers) {
			while (server == null) {
				if (serverIterator == null || !serverIterator.hasNext()) {
					if (servers.size() == 0) {
						callback.onServerConnectFailed();
						close();
						throw new NatsEmptyServerListException();
					}
					serverIterator = servers.iterator();
				}
				server = serverIterator.next();
				if (maxReconnectAttempts > 0 && server.getConnectionAttempts() > maxReconnectAttempts) {
					logger.log(NatsLogger.Level.WARNING, "Exceeded max connection attempts connecting to Nats server " + server.address);
					callback.onServerReconnectFailed(server.address);
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
					channelWrite(encodeConnect(finalServer));
				} else {
					finalServer.incConnectionAttempts();
				}
			}
		});

	}

	public void close() {
		closed = true;
		if (channel.isConnected()) {
			channel.close().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					cleanupResources();
				}
			});
		} else {
			cleanupResources();
		}
	}

	private void cleanupResources() {
		if (createChannelFactory) {
			channelFactory.releaseExternalResources();
		}
		if (timer != null) {
			timer.cancel();
		}
		NatsClosedException closedException = new NatsClosedException();
		synchronized (publishQueue) {
			for (Publish publish : publishQueue) {
				publish.future.setDone(closedException);
			}
		}
	}

	public NatsFuture publish(String subject, String message) {
		return publish(subject, message, null);
	}
	
	public NatsFuture publish(String subject, String message, String replyTo) {
		assertNatsOpen();

		String publishCommand = encodePublish(subject, replyTo, message);
		NatsFutureImpl future = new NatsFutureImpl();
		synchronized (publishQueue) {
			if (channel.isConnected()) {
				writePublishCommand(publishCommand, future);
			} else {
				publishQueue.add(new Publish(publishCommand, future));
			}
		}
		return future;
	}

	public NatsFuture ping() {
		assertNatsOpen();
		final NatsFutureImpl future = new NatsFutureImpl();
		if (channel.isConnected()) {
			pongQueue.add(future);
			channelWrite(CMD_PING).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture channelFuture) throws Exception {
					if (!channelFuture.isSuccess()) {
						future.setDone(channelFuture.getCause());
					}
				}
			});
		} else {
			future.setDone(new NatsClosedException());
		}
		return future;
	}
	
	private ChannelFuture channelWrite(String command) {
		logger.log(NatsLogger.Level.DEBUG, "Client message: " + command);
		return channel.write(command);
	}
	
	private void writePublishCommand(String publishCommand, final NatsFutureImpl future) {
		channelWrite(publishCommand).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				future.setDone(channelFuture.getCause());
			}
		});
	}

	public RequestFuture request(String subject, String message) {
		return request(subject, message, null);
	}

	private RequestFuture request(String subject, String message, final Integer maxReplies) {
		assertNatsOpen();
		final String inbox = createInbox();
		final Subscription subscription = subscribe(inbox, maxReplies);
		final NatsFuture natsFuture = publish(subject, message, inbox);
		return new RequestFuture() {
			@Override
			public HandlerRegistration addCompletionHandler(CompletionHandler listener) {
				return natsFuture.addCompletionHandler(listener);
			}

			@Override
			public boolean isDone() {
				return natsFuture.isDone();
			}

			@Override
			public boolean isSuccess() {
				return natsFuture.isSuccess();
			}

			@Override
			public Throwable getCause() {
				return natsFuture.getCause();
			}

			@Override
			public void await() throws InterruptedException {
				natsFuture.await();
			}

			@Override
			public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
				return natsFuture.await(timeout, unit);
			}

			@Override
			public void close() {
				subscription.close();
			}

			@Override
			public String getSubject() {
				return inbox;
			}

			@Override
			public HandlerRegistration addMessageHandler(MessageHandler messageHandler) {
				return subscription.addMessageHandler(messageHandler);
			}

			@Override
			public Nats getNats() {
				return Nats.this;
			}

			@Override
			public int getReceivedMessages() {
				return subscription.getReceivedMessages();
			}

			@Override
			public Integer getMaxMessages() {
				return maxReplies;
			}

			@Override
			public String getQueueGroup() {
				return null;
			}
		};
	}

	public Subscription subscribe(String subject) {
		return subscribe(subject, null, null);
	}
	
	public Subscription subscribe(String subject, String queueGroup) {
		return subscribe(subject, queueGroup, null);
	}
	
	public Subscription subscribe(String subject, Integer maxMessages) {
		return subscribe(subject, null, maxMessages);
	}
	
	public Subscription subscribe(final String subject, final String queueGroup, final Integer maxMessages) {
		assertNatsOpen();
		// TODO Validate subject and queueGroup -- If they have white space it will break the protocol -- What is valid? -- Can't be empty.
		// TODO What's the queueGroup even for?
		final Integer id = subscriptionId.incrementAndGet();
		NatsSubscription subscription = new NatsSubscription() {
			final AtomicInteger receivedMessages = new AtomicInteger();
			private final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
			@Override
			public void close() {
				synchronized (subscriptions) {
					subscriptions.remove(id);
				}
				if (maxMessages == null) {
					channelWrite(encodeUnsubscribe(id, maxMessages));
				}
			}

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public Nats getNats() {
				return Nats.this;
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
			public void onMessage(String message, String replyTo) {
				final int messageCount = receivedMessages.incrementAndGet();
				if (maxMessages != null && messageCount >= maxMessages) {
					close();
				}
				synchronized (handlers) {
					for (MessageHandler handler : handlers) {
						handler.onMessage(this, message, replyTo);
					}
				}
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
		if (channel.isConnected()) {
			channelWrite(encodeSubscribeCommand(subscription));
			if (subscription.getMaxMessages() != null) {
				channelWrite(encodeUnsubscribe(subscription.getId(), subscription.getMaxMessages() - subscription.getReceivedMessages()));
			}
		}
	}

	private void assertNatsOpen() {
		if (closed) {
			throw new NatsClosedException();
		}
	}

	private String encodeConnect(NatsServer server) {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_CONNECT);
		builder.append(" {");
		if (server.user != null) {
			appendJsonField(builder, "user", server.user);
			appendJsonField(builder, "pass", server.password);
		}
		appendJsonField(builder, "verbose", Boolean.toString(verbose));
		appendJsonField(builder, "pedantic", Boolean.toString(pedantic));
		builder.append("}\r\n");
		return builder.toString();
	}

	private void appendJsonField(StringBuilder builder, String field, String value) {
		if (builder.length() > CMD_CONNECT.length() + 2) {
			builder.append(',');
		}
		// TODO We need some real JSON encoding to escape the values properly
		builder.append('"').append(field).append('"').append(':').append('"').append(value).append('"');
	}

	private String encodePublish(String subject, String replyTo, String message) {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_PUBLISH).append(' ').append(subject).append(' ');
		if (replyTo != null) {
			builder.append(replyTo).append(' ');
		}
		builder.append(message.length()).append("\r\n").append(message).append("\r\n");
		return builder.toString();
	}

	private String encodeSubscribeCommand(NatsSubscription subscription) {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_SUBSCRIBE).append(' ').append(subscription.getSubject()).append(' ');
		if (subscription.getQueueGroup() != null) {
			builder.append(subscription.getQueueGroup()).append(' ');
		}
		builder.append(subscription.getId()).append("\r\n");
		return builder.toString();
	}
	
	private String encodeUnsubscribe(Integer id, Integer maxMessages) {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_UNSUBSCRIBE).append(' ').append(id);
		if (maxMessages != null) {
			builder.append(' ').append(maxMessages);
		}
		builder.append("\r\n");
		return builder.toString();
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
				host = DEFAULT_HOST;
			} else {
				host = uri.getHost();
			}
			if (uri.getPort() > 0) {
				port = uri.getPort();
			} else {
				port = DEFAULT_PORT;
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
		void onMessage(String message, String replyTo);
		Integer getId();
	}

	private static final HandlerRegistration EMPTY_HANDLER_REGISTRATION = new HandlerRegistration() {
		@Override
		public void remove() {
			// DO nothing.
		}
	};

	private class NatsFutureImpl implements NatsFuture {
		private boolean done;
		private Throwable cause;
		private List<PublishHandlerRegistration> listeners;
		private final Object lock = new Object();
		@Override
		public HandlerRegistration addCompletionHandler(final CompletionHandler listener) {
			synchronized (lock) {
				if (done) {
					invokeListener(listener);
					return EMPTY_HANDLER_REGISTRATION;
				} else {
					PublishHandlerRegistration registration = new PublishHandlerRegistration(listener) {
						@Override
						public void remove() {
							synchronized (lock) {
								listeners.remove(this);
							}
						}
					};
					if (listeners == null) {
						listeners = new LinkedList<PublishHandlerRegistration>();
					}
					listeners.add(registration);
					return registration;
				}
			}
		}

		@Override
		public boolean isDone() {
			synchronized (lock) {
				return done;
			}
		}

		@Override
		public boolean isSuccess() {
			synchronized (lock) {
				return cause == null;
			}
		}

		@Override
		public Throwable getCause() {
			synchronized (lock) {
				return cause;
			}
		}

		@Override
		public void await() throws InterruptedException {
			synchronized (lock) {
				lock.wait();
			}
		}

		@Override
		public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
			synchronized (lock) {
				lock.wait(unit.toMillis(timeout));
				return isDone();
			}
		}

		void setDone(Throwable cause) {
			synchronized (lock) {
				if (done) {
					return;
				}
				done = true;
				this.cause = cause;
				if (listeners != null) {
					for (PublishHandlerRegistration handler : listeners) {
						invokeListener(handler.getCompletionHandler());
					}
				}
				lock.notifyAll();
			}
		}

		private void invokeListener(CompletionHandler listener) {
			try {
				listener.onComplete(this);
			} catch (Throwable t) {
				callback.onException(t);
			}
		}
	}

	private static abstract class PublishHandlerRegistration implements HandlerRegistration {

		private final CompletionHandler completionHandler;

		private PublishHandlerRegistration(CompletionHandler completionHandler) {
			this.completionHandler = completionHandler;
		}

		public CompletionHandler getCompletionHandler() {
			return completionHandler;
		}
	}
	
	private static class Publish {
		private final String publishCommand;
		private final NatsFutureImpl future;

		private Publish(String publishCommand, NatsFutureImpl future) {
			this.publishCommand = publishCommand;
			this.future = future;
		}
	}
	
}
