package jnats.codec;

import org.jboss.netty.channel.ChannelHandlerContext;

import java.util.Collection;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
// TODO Determine if we really need this class
public class ClientChannelHandler extends AbstractClientChannelHandler {

	@Override
	protected void handleOrphanedPings(ChannelHandlerContext ctx, Collection<ClientPingMessage> pings) {
		// Do nothing
	}

	@Override
	protected void handleOrphanedRequests(ChannelHandlerContext ctx, Collection<ClientRequest> requests) {
		// Do nothing
	}

	@Override
	public void publishedMessage(ChannelHandlerContext ctx, ServerPublishMessage message) {
		// Do nothing
	}

	@Override
	public void pongResponse(ChannelHandlerContext ctx, ClientPingMessage pingMessage, ServerPongMessage pongMessage) {
		// Do nothing
	}

	@Override
	public void serverInfo(ChannelHandlerContext ctx, ServerInfoMessage infoMessage) {
		// Do nothing
	}

	@Override
	public void okResponse(ChannelHandlerContext ctx, ClientRequest clientRequest, ServerOkMessage okMessage) {
		// Do nothing
	}

	@Override
	public void errorResponse(ChannelHandlerContext ctx, ClientRequest request, ServerErrorMessage errorMessage) {
		// Do nothing
	}
}
