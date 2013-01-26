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
package nats.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractClientInboundMessageHandlerAdapter extends ChannelInboundMessageHandlerAdapter<ServerFrame> {

	@Override
	public void messageReceived(ChannelHandlerContext context, ServerFrame frame) throws Exception {
		if (frame instanceof ServerPublishFrame) {
			publishedMessage(context, (ServerPublishFrame) frame);
		} else if (frame instanceof ServerPingFrame) {
			serverPing(context);
		} else if (frame instanceof ServerOkFrame) {
			okResponse(context, (ServerOkFrame) frame);
		} else if (frame instanceof ServerErrorFrame) {
			errorResponse(context, (ServerErrorFrame) frame);
		} else if (frame instanceof ServerPongFrame) {
			pongResponse(context, (ServerPongFrame) frame);
		} else if (frame instanceof ServerInfoFrame) {
			serverInfo(context, (ServerInfoFrame) frame);
		} else {
			throw new Error("Received a server response of an unknown type: " + frame.getClass().getName());
		}
	}

	protected abstract void publishedMessage(ChannelHandlerContext context, ServerPublishFrame frame);

	protected void serverPing(ChannelHandlerContext context) {
		context.write(ClientPongFrame.PONG);
	}

	protected abstract void pongResponse(ChannelHandlerContext context, ServerPongFrame pongFrame);

	protected abstract void serverInfo(ChannelHandlerContext context, ServerInfoFrame infoFrame);

	// TODO Determine that this only gets called in response to the CONNECT
	protected abstract void okResponse(ChannelHandlerContext context, ServerOkFrame okFrame);

	protected abstract void errorResponse(ChannelHandlerContext ctx, ServerErrorFrame errorFrame);

}
