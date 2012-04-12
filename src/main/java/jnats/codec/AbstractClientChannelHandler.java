package jnats.codec;

import jnats.NatsException;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public abstract class AbstractClientChannelHandler extends SimpleChannelHandler {

	// Access must be synchronized on self.
	private final Queue<ClientPingMessage> pingQueue = new LinkedList<ClientPingMessage>();

	// Access must be synchronized on self.
	private final Queue<ClientRequest> requestQueue = new LinkedList<ClientRequest>();

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof ServerMessage) {
			if (e.getMessage() instanceof ServerPublishMessage) {
				publishedMessage(ctx, (ServerPublishMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerPingMessage) {
				serverPing(ctx);
			} else if (e.getMessage() instanceof  ServerOkMessage) {
				okResponse(ctx, pollRequestQueue(), (ServerOkMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerErrorMessage) {
				errorResponse(ctx, pollRequestQueue(), (ServerErrorMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerPongMessage) {
				pongResponse(ctx, pollPingQueue(), (ServerPongMessage) e.getMessage());
			} else if (e.getMessage() instanceof ServerInfoMessage) {
				serverInfo(ctx, (ServerInfoMessage) e.getMessage());
			} else {
				throw new Error("Received a server response of an unknown type: " + e.getMessage().getClass().getName());
			}
		} else {
			super.messageReceived(ctx, e);
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		if (e.getMessage() instanceof ClientPingMessage) {
			synchronized (pingQueue) {
				pingQueue.add((ClientPingMessage) e.getMessage());
			}
		} else if (e.getMessage() instanceof ClientRequest) {
			synchronized (requestQueue) {
				requestQueue.add((ClientRequest) e.getMessage());
			}
		}
		super.writeRequested(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		synchronized (pingQueue) {
			handleOrphanedPings(ctx, pingQueue);
			pingQueue.clear();
		}
		synchronized (requestQueue) {
			handleOrphanedRequests(ctx, requestQueue);
			requestQueue.clear();
		}
	}

	protected abstract void handleOrphanedPings(ChannelHandlerContext ctx, Collection<ClientPingMessage> pings);

	protected abstract void handleOrphanedRequests(ChannelHandlerContext ctx, Collection<ClientRequest> requests);

	protected abstract void publishedMessage(ChannelHandlerContext ctx, ServerPublishMessage message);

	protected void serverPing(ChannelHandlerContext ctx) {
		ctx.getChannel().write(ClientPongMessage.PONG_MESSAGE);
	}

	protected abstract void pongResponse(ChannelHandlerContext ctx, ClientPingMessage pingMessage, ServerPongMessage pongMessage);

	protected abstract void serverInfo(ChannelHandlerContext ctx, ServerInfoMessage infoMessage);

	protected abstract void okResponse(ChannelHandlerContext ctx, ClientRequest request, ServerOkMessage okMessage);

	protected abstract void errorResponse(ChannelHandlerContext ctx, ClientRequest request, ServerErrorMessage errorMessage);

	private ClientRequest pollRequestQueue() {
		final ClientRequest request;
		synchronized (requestQueue) {
			request = requestQueue.poll();
		}
		if (request == null) {
			throw new NatsException("Received a response from the NATS server for an unknown request.");
		}
		return request;
	}

	private ClientPingMessage pollPingQueue() {
		final ClientPingMessage ping;
		synchronized (pingQueue) {
			ping = pingQueue.poll();
		}
		if (ping == null) {
			throw new NatsException("Received a pong from the NATS server for an unknown ping.");
		}
		return ping;
	}

}
