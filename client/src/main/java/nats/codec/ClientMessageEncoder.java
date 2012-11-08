package nats.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import nats.NatsException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientMessageEncoder extends MessageToByteEncoder<ClientMessage> {

	private static final Charset UTF8 = Charset.forName("utf-8");

	public static final ByteBuf CMD_CONNECT = Unpooled.copiedBuffer("CONNECT", UTF8);
	public static final ByteBuf CMD_PUBLISH = Unpooled.copiedBuffer("PUB", UTF8);
	public static final ByteBuf CMD_SUBSCRIBE = Unpooled.copiedBuffer("SUB", UTF8);
	public static final ByteBuf CMD_UNSUBSCRIBE = Unpooled.copiedBuffer("UNSUB", UTF8);

	public static final ByteBuf PING = Unpooled.copiedBuffer("PING\r\n", UTF8);
	public static final ByteBuf PONG = Unpooled.copiedBuffer("PONG\r\n", UTF8);

	private final ObjectMapper mapper;

	public ClientMessageEncoder() {
		mapper = new ObjectMapper();
	}

	@Override
	public void encode(ChannelHandlerContext ctx, ClientMessage msg, ByteBuf out) throws Exception {
		if (msg instanceof ClientConnectMessage) {
			final ClientConnectMessage message = (ClientConnectMessage) msg;
			out.writeBytes(CMD_CONNECT);
			out.writeByte(' ');
			mapper. writeValue(new ByteBufOutputStream(out), message.getBody());
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ClientPingMessage) {
			out.writeBytes(PING);
		} else if (msg instanceof ClientPongMessage) {
			out.writeBytes(PONG);
		} else if (msg instanceof ClientPublishMessage) {
			final ClientPublishMessage message = (ClientPublishMessage) msg;
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
		} else if (msg instanceof ClientSubscribeMessage) {
			final ClientSubscribeMessage message = (ClientSubscribeMessage) msg;
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
		} else if (msg instanceof ClientUnsubscribeMessage) {
			final ClientUnsubscribeMessage message = (ClientUnsubscribeMessage) msg;
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
