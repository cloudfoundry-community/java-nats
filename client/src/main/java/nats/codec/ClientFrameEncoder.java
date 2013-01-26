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
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import nats.NatsException;
import org.codehaus.jackson.map.ObjectMapper;

import java.nio.charset.Charset;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientFrameEncoder extends MessageToByteEncoder<ClientFrame> {

	private static final Charset UTF8 = Charset.forName("utf-8");

	public static final byte[] CMD_CONNECT = "CONNECT".getBytes(UTF8);
	public static final byte[] CMD_PUBLISH = "PUB".getBytes(UTF8);
	public static final byte[] CMD_SUBSCRIBE = "SUB".getBytes(UTF8);
	public static final byte[] CMD_UNSUBSCRIBE = "UNSUB".getBytes(UTF8);

	public static final byte[] PING = "PING\r\n".getBytes(UTF8);
	public static final byte[] PONG = "PONG\r\n".getBytes(UTF8);

	private final ObjectMapper mapper;

	public ClientFrameEncoder() {
		mapper = new ObjectMapper();
	}

	@Override
	public void encode(ChannelHandlerContext ctx, ClientFrame msg, ByteBuf out) throws Exception {
		if (msg instanceof ClientConnectFrame) {
			final ClientConnectFrame message = (ClientConnectFrame) msg;
			out.writeBytes(CMD_CONNECT);
			out.writeByte(' ');
			mapper. writeValue(new ByteBufOutputStream(out), message.getBody());
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ClientPingFrame) {
			out.writeBytes(PING);
		} else if (msg instanceof ClientPongFrame) {
			out.writeBytes(PONG);
		} else if (msg instanceof ClientPublishFrame) {
			final ClientPublishFrame message = (ClientPublishFrame) msg;
			out.writeBytes(CMD_PUBLISH);
			out.writeByte(' ');

			out.writeBytes(message.getSubject().getBytes(UTF8));
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
		} else if (msg instanceof ClientSubscribeFrame) {
			final ClientSubscribeFrame message = (ClientSubscribeFrame) msg;
			out.writeBytes(CMD_SUBSCRIBE);
			out.writeByte(' ');
			out.writeBytes(message.getSubject().getBytes(UTF8));
			out.writeByte(' ');
			final String queueGroup = message.getQueueGroup();
			if (queueGroup != null) {
				out.writeBytes(queueGroup.getBytes(UTF8));
				out.writeByte(' ');
			}
			out.writeBytes(message.getId().getBytes(UTF8));
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ClientUnsubscribeFrame) {
			final ClientUnsubscribeFrame message = (ClientUnsubscribeFrame) msg;
			out.writeBytes(CMD_UNSUBSCRIBE);
			out.writeByte(' ');
			out.writeBytes(message.getId().getBytes(UTF8));
			final Integer maxMessages = message.getMaxMessages();
			if (maxMessages != null) {
				out.writeByte(' ');
				ByteBufUtil.writeIntegerAsString(out, maxMessages);
			}
			out.writeBytes(ByteBufUtil.CRLF);
		} else {
			throw new NatsException("Unable to encode client message of type " + msg.getClass().getName());
		}
	}
}
