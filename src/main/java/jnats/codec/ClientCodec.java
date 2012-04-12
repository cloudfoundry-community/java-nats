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
package jnats.codec;

import jnats.NatsServerException;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodec implements ChannelUpstreamHandler, ChannelDownstreamHandler {

	// Regular expressions used for parsing server messages
	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s+('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	private boolean waitingMessagePayload = false;
	private ChannelHandler delimiterBasedFrameDecoder;
	private ServerPublishMessage message;

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) e;
			final Object message = messageEvent.getMessage();
			if (message instanceof String) {
				if (waitingMessagePayload) {
					handleMessagePayload(ctx, (String) message);
				} else {
					handleCommand(ctx, (String) message);
				}
				return;
			}
		}
		ctx.sendUpstream(e);
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if (e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent) e;
			if (messageEvent.getMessage() instanceof ClientMessage) {
				ClientMessage message = (ClientMessage) messageEvent.getMessage();
				final String encodedMessage = message.encode();
				Channels.write(ctx, e.getFuture(), encodedMessage, messageEvent.getRemoteAddress());
				return;
			}
		}
		ctx.sendDownstream(e);
	}

	private void handleCommand(ChannelHandlerContext ctx, String command) {
		Matcher matcher = MSG_PATTERN.matcher(command);
		if (matcher.matches()) {
			final String subject = matcher.group(1);
			final int id = Integer.valueOf(matcher.group(2));
			// TODO Verify that this is really the queue group -- I don't think it is
			final String queueGroup = matcher.group(3);
			final String replyTo = matcher.group(4);
			final int length = Integer.valueOf(matcher.group(5));
			message = new ServerPublishMessage(id, subject, queueGroup, replyTo);
			if (length == 0) {
				Channels.fireMessageReceived(ctx, message);
			} else {
				delimiterBasedFrameDecoder = ctx.getPipeline().replace(
						ClientChannelPipelineFactory.PIPELINE_FRAME_DECODER,
						ClientChannelPipelineFactory.PIPELINE_FIXED_DECODER,
						new FixedLengthFrameDecoder(length));
				waitingMessagePayload = true;
			}
			return;
		}
		matcher = INFO_PATTERN.matcher(command);
		if (matcher.matches()) {
			Channels.fireMessageReceived(ctx, new ServerInfoMessage(matcher.group(1)));
			return;
		}
		matcher = OK_PATTERN.matcher(command);
		if (matcher.matches()) {
			Channels.fireMessageReceived(ctx, ServerOkMessage.OK_MESSAGE);
			return;
		}
		matcher = ERR_PATTERN.matcher(command);
		if (matcher.matches()) {
			Channels.fireMessageReceived(ctx, new ServerErrorMessage(matcher.group(1)));
			return;
		}
		if (PING_PATTERN.matcher(command).matches()) {
			Channels.fireMessageReceived(ctx, new ServerPingMessage());
			return;
		}
		if (PONG_PATTERN.matcher(command).matches()) {
			Channels.fireMessageReceived(ctx, new ServerPongMessage());
			return;
		}
		throw new NatsServerException("Don't know how to handle the following sent by the Nats server: " + command);
	}

	private void handleMessagePayload(ChannelHandlerContext ctx, String payload) {
		try {
			message.setBody(payload);
			Channels.fireMessageReceived(ctx, message);
		} finally {
			waitingMessagePayload = false;
			ctx.getPipeline().replace(ClientChannelPipelineFactory.PIPELINE_FIXED_DECODER, ClientChannelPipelineFactory.PIPELINE_FRAME_DECODER, delimiterBasedFrameDecoder);
		}
	}

}
