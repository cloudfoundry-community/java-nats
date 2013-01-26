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
import io.netty.handler.codec.MessageToByteEncoder;
import nats.NatsException;

import java.nio.charset.Charset;

/**
 * Encodes {@link ServerFrame} objects to binary to be sent over the network.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerFrameEncoder extends MessageToByteEncoder<ServerFrame> {

	private static final Charset UTF8 = Charset.forName("utf-8");

	public static final byte[] CMD_PUB = "MSG".getBytes(UTF8);
	public static final byte[] CMD_ERR = "-ERR".getBytes(UTF8);
	public static final byte[] CMD_INFO = "INFO".getBytes(UTF8);
	public static final byte[] OK = "+OK\r\n".getBytes(UTF8);
	public static final byte[] PING = "PING\r\n".getBytes(UTF8);
	public static final byte[] PONG = "PONG\r\n".getBytes(UTF8);

	@Override
	public void encode(ChannelHandlerContext ctx, ServerFrame msg, ByteBuf out) throws Exception {
		if (msg instanceof ServerPublishFrame) {
			final ServerPublishFrame message = (ServerPublishFrame) msg;
			out.writeBytes(CMD_PUB);
			out.writeByte(' ');
			out.writeBytes(message.getSubject().getBytes(UTF8));
			out.writeByte(' ');
			out.writeBytes(message.getId().getBytes(UTF8));
			out.writeByte(' ');
			final String replyTo = message.getReplyTo();
			if (replyTo != null) {
				out.writeBytes(replyTo.getBytes(UTF8));
				out.writeByte(' ');
			}
			final byte[] bodyBytes = message.getBody().getBytes(UTF8);
			ByteBufUtil.writeIntegerAsString(out, bodyBytes.length);
			out.writeBytes(ByteBufUtil.CRLF);
			out.writeBytes(bodyBytes);
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ServerErrorFrame) {
			final ServerErrorFrame message = (ServerErrorFrame) msg;
			final String errorMessage = message.getErrorMessage();
			out.writeBytes(CMD_ERR);
			if (errorMessage != null) {
				out.writeByte(' ');
				out.writeBytes(errorMessage.getBytes(UTF8));
			}
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ServerInfoFrame) {
			final ServerInfoFrame message = (ServerInfoFrame) msg;
			out.writeBytes(CMD_INFO);
			out.writeByte(' ');
			out.writeBytes(message.getInfo().getBytes(UTF8));
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ServerOkFrame) {
			out.writeBytes(OK);
		} else if (msg instanceof ServerPingFrame) {
			out.writeBytes(PING);
		} else if (msg instanceof ServerPongFrame) {
			out.writeBytes(PONG);
		} else {
			throw new NatsException("Unable to encode server of type " + msg.getClass().getName());
		}
	}

}
