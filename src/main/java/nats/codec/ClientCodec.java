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

import nats.Constants;
import nats.NatsServerException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a Netty codec for converting upstream {@link ChannelBuffer}s to {@link ServerMessage} objects and
 * downstream {@link ClientMessage} objects to {@code ChannelBuffer}s.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodec extends FrameDecoder implements ChannelDownstreamHandler {

	// Regular expressions used for parsing server messages
	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s+('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	private static final ChannelBuffer DELIMITER = ChannelBuffers.wrappedBuffer(new byte[] { '\r', '\n' });

	private final int maxMessageSize;

	private boolean waitingMessagePayload = false;
	private ServerPublishMessage message;
	private int payloadSize;

	public ClientCodec() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	public ClientCodec(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		if (waitingMessagePayload) {
			if (buffer.readableBytes() >= payloadSize + DELIMITER.capacity()) {
				String body = buffer.readBytes(payloadSize).toString(Charset.defaultCharset());
				buffer.skipBytes(DELIMITER.capacity());
				message.setBody(body);
				waitingMessagePayload = false;
				return message;
			}
		} else {
			int frameLength = indexOf(buffer, DELIMITER);
			if (frameLength >= 0) {
				if (frameLength > maxMessageSize) {
					buffer.skipBytes(frameLength + DELIMITER.capacity());
					throwTooLongFrameException(ctx);
				} else {
					String command = buffer.readBytes(frameLength).toString(Charset.defaultCharset());
					buffer.skipBytes(DELIMITER.capacity());
					ServerMessage serverMessage = decodeCommand(command);
					if (serverMessage != null) {
						return serverMessage;
					}
				}
			}
		}
		if (buffer.readableBytes() > maxMessageSize) {
			throwTooLongFrameException(ctx);
		}
		return null;
	}

	private void throwTooLongFrameException(ChannelHandlerContext ctx) {
		Channels.fireExceptionCaught(
				ctx.getChannel(),
				new TooLongFrameException("message size exceeds " + maxMessageSize));
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) e;
			if (messageEvent.getMessage() instanceof ClientMessage) {
				ClientMessage message = (ClientMessage) messageEvent.getMessage();
				final String encodedMessage = message.encode();
				final ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(encodedMessage.getBytes());
				Channels.write(ctx, e.getFuture(), buffer, messageEvent.getRemoteAddress());
				return;
			}
		}
		ctx.sendDownstream(e);
	}

	private ServerMessage decodeCommand(String command) {
		Matcher matcher = MSG_PATTERN.matcher(command);
		if (matcher.matches()) {
			final String subject = matcher.group(1);
			final int id = Integer.valueOf(matcher.group(2));
			// TODO Verify that this is really the queue group -- I don't think it is
			final String queueGroup = matcher.group(3);
			final String replyTo = matcher.group(4);
			final int length = Integer.valueOf(matcher.group(5));
			message = new ServerPublishMessage(id, subject, queueGroup, replyTo);
			waitingMessagePayload = true;
			payloadSize = length;
			return null;
		}
		matcher = INFO_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerInfoMessage(matcher.group(1));
		}
		matcher = OK_PATTERN.matcher(command);
		if (matcher.matches()) {
			return ServerOkMessage.OK_MESSAGE;
		}
		matcher = ERR_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerErrorMessage(matcher.group(1));
		}
		if (PING_PATTERN.matcher(command).matches()) {
			return new ServerPingMessage();
		}
		if (PONG_PATTERN.matcher(command).matches()) {
			return new ServerPongMessage();
		}
		throw new NatsServerException("Don't know how to handle the following sent by the Nats server: " + command);
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 *
	 *
	 * Copied from {@link org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder}.
	 */
	private static int indexOf(ChannelBuffer haystack, ChannelBuffer needle) {
		for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
			int haystackIndex = i;
			int needleIndex;
			for (needleIndex = 0; needleIndex < needle.capacity(); needleIndex++) {
				if (haystack.getByte(haystackIndex) != needle.getByte(needleIndex)) {
					break;
				} else {
					haystackIndex++;
					if (haystackIndex == haystack.writerIndex() &&
							needleIndex != needle.capacity() - 1) {
						return -1;
					}
				}
			}

			if (needleIndex == needle.capacity()) {
				// Found the needle from the haystack!
				return i - haystack.readerIndex();
			}
		}
		return -1;
	}

}
