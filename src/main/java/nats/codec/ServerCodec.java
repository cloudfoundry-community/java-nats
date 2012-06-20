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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerCodec  extends AbstractCodec implements ChannelDownstreamHandler {

	private ClientPublishMessage message;

	public ServerCodec() {
		super();
	}

	@Override
	protected ClientMessage decodeCommand(String command) {
		// CONNECT
		if (command.startsWith(ClientConnectMessage.CMD_CONNECT)) {
			final String body = command.substring(ClientConnectMessage.CMD_CONNECT.length()).trim();
			final ConnectBody connectBody = ConnectBody.parse(body);
			return new ClientConnectMessage(connectBody);
		}

		// PING
		if (ClientPingMessage.CMD_PING.equals(command.trim())) {
			return ClientPingMessage.PING;
		}

		// PONG
		if (ClientPongMessage.CMD_PONG.equals(command.trim())) {
			return ClientPongMessage.PONG;
		}

		// PUB
		if (command.startsWith(ClientPublishMessage.CMD_PUBLISH)) {
			try {
				final String[] parts = command.substring(ClientPublishMessage.CMD_PUBLISH.length()).trim().split("\\s+");
				if (parts.length < 2 || parts.length > 3) {
					throw new NatsDecodingException(command);
				}
				final String subject = parts[0];
				final int length = Integer.parseInt(parts[parts.length - 1]);
				final String replyTo = (parts.length == 3) ? parts[1] : null;
				this.message = new ClientPublishMessage(subject, replyTo);
				expectBody(length);
				return null;
			} catch (NumberFormatException e) {
				throw new NatsDecodingException(command);
			}
		}

		// SUB
		if (command.startsWith(ClientSubscribeMessage.CMD_SUBSCRIBE)) {
			final String[] parts = command.substring(ClientSubscribeMessage.CMD_SUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 2 || parts.length > 3) {
				throw new NatsDecodingException(command);
			}
			final String subject = parts[0];
			final String id = parts[parts.length -1];
			final String queueGroup = (parts.length == 3) ? parts[1] : null;
			return new ClientSubscribeMessage(id, subject, queueGroup);
		}

		// UNSUB
		if (command.startsWith(ClientUnsubscribeMessage.CMD_UNSUBSCRIBE)) {
			final String[] parts = command.substring(ClientUnsubscribeMessage.CMD_UNSUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 1 || parts.length > 2) {
				throw new NatsDecodingException(command);
			}
			final String id = parts[0];
			final Integer maxMessages = (parts.length == 2) ? Integer.valueOf(parts[1]) : null;
			return new ClientUnsubscribeMessage(id, maxMessages);
		}

		throw new NatsDecodingException(command);
	}

	@Override
	protected ClientMessage handleBody(String body) {
		message.setBody(body);
		return message;
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) e;
			if (messageEvent.getMessage() instanceof ServerMessage) {
				ServerMessage message = (ServerMessage) messageEvent.getMessage();
				final ChannelBuffer buffer = message.encode();
				Channels.write(ctx, e.getFuture(), buffer, messageEvent.getRemoteAddress());
				return;
			}
		}
		ctx.sendDownstream(e);
	}
}
