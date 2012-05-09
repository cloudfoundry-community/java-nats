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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a Netty codec for converting upstream {@link ChannelBuffer}s to {@link ServerMessage} objects and
 * downstream {@link ClientMessage} objects to {@code ChannelBuffer}s.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodec extends AbstractCodec implements ChannelDownstreamHandler {

	// Regular expressions used for parsing server messages
	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s+('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	private ServerPublishMessage message;

	public ClientCodec() {
		super();
	}

	public ClientCodec(int maxMessageSize) {
		super(maxMessageSize);
	}

	@Override
	protected Object handleBody(String body) {
		message.setBody(body);
		return message;
	}

	protected ServerMessage decodeCommand(String command) {
		Matcher matcher = MSG_PATTERN.matcher(command);
		if (matcher.matches()) {
			final String subject = matcher.group(1);
			final String id = matcher.group(2);
			// TODO Verify that this is really the queue group -- I don't think it is
			final String queueGroup = matcher.group(3);
			final String replyTo = matcher.group(4);
			final int length = Integer.valueOf(matcher.group(5));
			message = new ServerPublishMessage(id, subject, queueGroup, replyTo);
			expectBody(length);
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
		throw new NatsDecodingException(command);
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) e;
			if (messageEvent.getMessage() instanceof ClientMessage) {
				ClientMessage message = (ClientMessage) messageEvent.getMessage();
				final ChannelBuffer buffer = message.encode();
				Channels.write(ctx, e.getFuture(), buffer, messageEvent.getRemoteAddress());
				return;
			}
		}
		ctx.sendDownstream(e);
	}


}
