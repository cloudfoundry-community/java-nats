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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import nats.NatsException;
import nats.codec.AbstractClientInboundMessageHandlerAdapter;
import nats.codec.ClientCodec;
import nats.codec.ClientConnectFrame;
import nats.codec.ClientPublishFrame;
import nats.codec.ClientSubscribeFrame;
import nats.codec.ConnectBody;
import nats.codec.ServerErrorFrame;
import nats.codec.ServerInfoFrame;
import nats.codec.ServerOkFrame;
import nats.codec.ServerPongFrame;
import nats.codec.ServerPublishFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class NatsImpl implements Nats {

	private static final Logger LOGGER = LoggerFactory.getLogger(NatsImpl.class);

	private final EventLoopGroup eventLoopGroup;

	private final boolean shutDownEventLoop;

	/**
	 * The current Netty {@link Channel} used for communicating with the NATS server. This field should never be null
	 * after the constructor has finished.
	 */
	// TODO Audit that closed and lock are accessed with lock
	// Must hold monitor #lock to access
	private Channel channel;

	// Must hold monitor #lock to access
	private boolean closed = false;

	private final Object lock = new Object();

	private boolean serverReady = false;

	// Configuration values
	private final boolean automaticReconnect;
	private final long reconnectTimeWait;
	private final boolean pedantic;
	private final int maxFrameSize;

	private final ServerList serverList = new ServerList();

	/**
	 * Holds the publish commands that have been queued up due to the connection being down.
	 * <p/>
	 * <p>Must hold monitor #lock to access this queue.
	 */
	private final Queue<ClientPublishFrame> publishQueue = new LinkedList<>();

	/**
	 * Holds the list of subscriptions held by this {@code Nats} instance.
	 * <p/>
	 * <p>Must hold monitor #lock to access.
	 */
	private final Map<String, NatsSubscription> subscriptions = new HashMap<>();

	/**
	 * Counter used for obtaining subscription ids. Each subscription must have its own unique id that is sent to the
	 * NATS server to uniquely identify each subscription..
	 */
	private final AtomicInteger subscriptionId = new AtomicInteger();

	/**
	 * Generates a random string used for creating a unique string. The {@code request} methods rely on this
	 * functionality.
	 *
	 * @return a unique random string.
	 */
	public static String createInbox() {
		byte[] bytes = new byte[16];
		ThreadLocalRandom.current().nextBytes(bytes);
		return "_INBOX." + new BigInteger(bytes).abs().toString(16);
	}

	NatsImpl(NatsConnector connector) {
		shutDownEventLoop = connector.eventLoopGroup == null;
		eventLoopGroup =  shutDownEventLoop ? new NioEventLoopGroup() : connector.eventLoopGroup;

		// Setup server list
		serverList.addServers(connector.hosts);

		// Set parameters
		automaticReconnect = connector.automaticReconnect;
		reconnectTimeWait = connector.reconnectWaitTime;
		pedantic = connector.pedantic;
		maxFrameSize = connector.maxFrameSize;

		// Start connection to server
		connect();
	}

	private void connect() {
		synchronized (lock) {
			if (closed) {
				return;
			}
		}
		final ServerList.Server server = serverList.nextServer();
		LOGGER.debug("Attempting to connect to {} with user {}", server.getAddress(), server.getUser());
		new Bootstrap()
				.group(eventLoopGroup)
				.remoteAddress(server.getAddress())
				.channel(NioSocketChannel.class)
				.handler(new NatsChannelInitializer())
				.connect().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					LOGGER.debug("Connection to {} successful", server.getAddress());
					server.connectionSuccess();
					synchronized (lock) {
						channel = future.channel();
						if (closed) {
							channel.close();
						} else {
							channel.write(new ClientConnectFrame(new ConnectBody(server.getUser(), server.getPassword(), pedantic, false)));
						}
					}
				} else {
					LOGGER.warn("Connection to {} failed", server.getAddress());
					server.connectionFailure();
					scheduleReconnect();
				}
			}
		});
	}

	private void scheduleReconnect() {
		synchronized (lock) {
			serverReady = false;
			if (!closed && automaticReconnect) {
				eventLoopGroup.next().schedule(new Runnable() {
					@Override
					public void run() {
						connect();
					}
				}, reconnectTimeWait, TimeUnit.MILLISECONDS);
			}
		}
	}

	@Override
	public boolean isConnected() {
		synchronized (lock) {
			return channel != null && channel.isActive();
		}
	}

	@Override
	public boolean isClosed() {
		synchronized (lock) {
			return closed;
		}
	}

	@Override
	public void close() {
		synchronized (lock) {
			closed = true;
			serverReady = false;
			if (channel != null) {
				channel.close();
			}
			if (shutDownEventLoop) {
				eventLoopGroup.shutdown();
			}
			// We have to make a copy of the subscriptions because calling subscription.close() modifies the subscriptions map.
			Collection<Subscription> subscriptionsCopy = new ArrayList<Subscription>(subscriptions.values());
			for (Subscription subscription : subscriptionsCopy) {
				subscription.close();
			}
		}
	}

	@Override
	public void publish(String subject) {
		publish(subject, "", null);
	}

	@Override
	public void publish(String subject, String body) {
		publish(subject, body, null);
	}

	@Override
	public void publish(String subject, String body, String replyTo) {
		assertNatsOpen();

		final ClientPublishFrame publishFrame = new ClientPublishFrame(subject, body, replyTo);
		publish(publishFrame);
	}

	private void publish(ClientPublishFrame publishFrame) {
		synchronized (lock) {
			if (serverReady) {
				channel.write(publishFrame);
			} else {
				publishQueue.add(publishFrame);
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
	public Subscription subscribe(String subject, String queueGroup, Integer maxMessages) {
		assertNatsOpen();
		final String id = Integer.toString(subscriptionId.incrementAndGet());
		final NatsSubscription subscription = new NatsSubscription(subject, queueGroup, maxMessages, id);

		synchronized (lock) {
			subscriptions.put(id, subscription);
			if (serverReady) {
				writeSubscription(subscription);
			}
		}
		return subscription;
	}

	private void writeSubscription(NatsSubscription subscription) {
		synchronized (lock) {
			if (serverReady) {
				channel.write(new ClientSubscribeFrame(subscription.getId(), subscription.getSubject(), subscription.getQueueGroup()));
			}
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
	public Request request(final String subject, String message, final Integer maxReplies, MessageHandler... messageHandlers) {
		assertNatsOpen();
		final String inbox = createInbox();
		final Subscription subscription = subscribe(inbox, maxReplies);
		for (MessageHandler handler : messageHandlers) {
			subscription.addMessageHandler(handler);
		}

		final ClientPublishFrame publishFrame = new ClientPublishFrame(subject, message, inbox);
		publish(publishFrame);
		return new Request() {
			@Override
			public void close() {
				subscription.close();
			}

			@Override
			public String getSubject() {
				return subject;
			}

			@Override
			public int getReceivedReplies() {
				return subscription.getReceivedMessages();
			}

			@Override
			public Integer getMaxReplies() {
				return maxReplies;
			}
		};
	}

	private void assertNatsOpen() {
		if (closed) {
			throw new NatsClosedException();
		}
	}

	private class NatsSubscription extends DefaultSubscription {
		final String id;
		protected NatsSubscription(String subject, String queueGroup, Integer maxMessages, String id) {
			super(subject, queueGroup, maxMessages);
			this.id = id;
		}

		String getId() {
			return id;
		}
	}

	private class NatsChannelInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			// TODO Add debug logging to codec
			final ChannelPipeline pipeline = channel.pipeline();
			pipeline.addLast("codec", new ClientCodec(maxFrameSize));
			pipeline.addLast("handler", new AbstractClientInboundMessageHandlerAdapter() {
				@Override
				protected void publishedMessage(ChannelHandlerContext context, ServerPublishFrame frame) {
					final NatsSubscription subscription;
					synchronized (lock) {
						subscription = subscriptions.get(frame.getId());
					}
					if (subscription == null) {
						throw new NatsException("Received a body for an unknown subscription.");
					}
					subscription.onMessage(frame.getSubject(), frame.getBody(), frame.getReplyTo());
				}

				@Override
				protected void pongResponse(ChannelHandlerContext context, ServerPongFrame pongFrame) {
					// Ignore
				}

				@Override
				protected void serverInfo(ChannelHandlerContext context, ServerInfoFrame infoFrame) {
					// TODO Parse info body for alternative servers to connect to as soon as NATS' clustering support starts sending this.
				}

				@Override
				protected void okResponse(ChannelHandlerContext context, ServerOkFrame okFrame) {
					synchronized (lock) {
					// Resubscribe when the channel opens.
						for (NatsSubscription subscription : subscriptions.values()) {
							writeSubscription(subscription);
						}
					// Resend pending publish commands.
						for (ClientPublishFrame publish : publishQueue) {
							context.write(publish);
						}
					}
				}

				@Override
				protected void errorResponse(ChannelHandlerContext ctx, ServerErrorFrame errorFrame) {
					throw new NatsException("Sever error: " + errorFrame.getErrorMessage());
				}

				@Override
				public void channelActive(ChannelHandlerContext ctx) throws Exception {
					// TODO Fire connection state changed
				}

				@Override
				public void channelInactive(ChannelHandlerContext ctx) throws Exception {
					// TODO Fire connection state changed
					// TODO Change flag to indicate that the server is not ready
					scheduleReconnect();
				}

				@Override
				public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
					LOGGER.error("Error", cause);
				}
			});
		}
	}
}
