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
package nats.codec;

import org.jboss.netty.channel.ChannelHandlerContext;

import java.util.Collection;

/**
 * @author Mike Heath <elcapo@gmail.com>
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
