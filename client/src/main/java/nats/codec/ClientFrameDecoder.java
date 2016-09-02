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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * @author Mike Heath
 */
public class ClientFrameDecoder extends AbstractFrameDecoder<ClientFrame> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientFrameDecoder.class);

	public static final String CMD_CONNECT = "CONNECT";
	public static final String CMD_PUBLISH = "PUB";
	public static final String CMD_SUBSCRIBE = "SUB";
	public static final String CMD_UNSUBSCRIBE = "UNSUB";

	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);

	@Override
	protected ClientFrame decodeCommand(ChannelHandlerContext context, String command, ByteBuf in) {
		LOGGER.trace("Decoding '{}'", command);

		// CONNECT
		if (command.startsWith(CMD_CONNECT)) {
			final String body = command.substring(CMD_CONNECT.length()).trim();
			final ConnectBody connectBody = ConnectBody.parse(body);
			return new ClientConnectFrame(connectBody);
		}

		// PING
		if (PING_PATTERN.matcher(command).matches()) {
			return ClientPingFrame.PING;
		}

		// PONG
		if (PONG_PATTERN.matcher(command).matches()) {
			return ClientPongFrame.PONG;
		}

		// PUB
		if (command.startsWith(CMD_PUBLISH)) {
			try {
				final String[] parts = command.substring(CMD_PUBLISH.length()).trim().split("\\s+");
				if (parts.length < 2 || parts.length > 3) {
					throw new NatsDecodingException(command);
				}
				final String subject = parts[0];
				final int length = Integer.parseInt(parts[parts.length - 1]);
				final String replyTo = (parts.length == 3) ? parts[1] : null;
				final byte[] bodyBytes = new byte[length];
				in.readBytes(bodyBytes, 0, length);
				in.skipBytes(ByteBufUtil.CRLF.length);
				final String body = new String(bodyBytes);
				return new ClientPublishFrame(subject, body, replyTo);
			} catch (NumberFormatException e) {
				throw new NatsDecodingException(command);
			}
		}

		// SUB
		if (command.startsWith(CMD_SUBSCRIBE)) {
			final String[] parts = command.substring(CMD_SUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 2 || parts.length > 3) {
				throw new NatsDecodingException(command);
			}
			final String subject = parts[0];
			final String id = parts[parts.length - 1];
			final String queueGroup = (parts.length == 3) ? parts[1] : null;
			return new ClientSubscribeFrame(id, subject, queueGroup);
		}

		// UNSUB
		if (command.startsWith(CMD_UNSUBSCRIBE)) {
			final String[] parts = command.substring(CMD_UNSUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 1 || parts.length > 2) {
				throw new NatsDecodingException(command);
			}
			final String id = parts[0];
			final Integer maxMessages = (parts.length == 2) ? Integer.valueOf(parts[1]) : null;
			return new ClientUnsubscribeFrame(id, maxMessages);
		}

		throw new NatsDecodingException(command);
	}

}
