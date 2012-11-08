package nats.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import nats.NatsException;

import java.nio.charset.Charset;

/**
 * Encodes {@link ServerMessage} objects to binary to be sent over the network.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerMessageEncoder extends MessageToByteEncoder<ServerMessage> {

	private static final Charset UTF8 = Charset.forName("utf-8");

	public static final ByteBuf CMD_PUB = Unpooled.copiedBuffer("MSG", UTF8);
	public static final ByteBuf CMD_ERR = Unpooled.copiedBuffer("-ERR", UTF8);
	public static final ByteBuf CMD_INFO = Unpooled.copiedBuffer("INFO", UTF8);
	public static final ByteBuf OK = Unpooled.copiedBuffer("+OK\r\n", UTF8);
	public static final ByteBuf PING = Unpooled.copiedBuffer("PING\r\n", UTF8);
	public static final ByteBuf PONG = Unpooled.copiedBuffer("PONG\r\n", UTF8);

	@Override
	public void encode(ChannelHandlerContext ctx, ServerMessage msg, ByteBuf out) throws Exception {
		if (msg instanceof ServerPublishMessage) {
			final ServerPublishMessage message = (ServerPublishMessage) msg;
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
		} else if (msg instanceof ServerErrorMessage) {
			final ServerErrorMessage message = (ServerErrorMessage) msg;
			final String errorMessage = message.getErrorMessage();
			out.writeBytes(CMD_ERR);
			if (errorMessage != null) {
				out.writeByte(' ');
				out.writeBytes(errorMessage.getBytes(UTF8));
			}
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ServerInfoMessage) {
			final ServerInfoMessage message = (ServerInfoMessage) msg;
			out.writeBytes(CMD_INFO);
			out.writeByte(' ');
			out.writeBytes(message.getInfo().getBytes(UTF8));
			out.writeBytes(ByteBufUtil.CRLF);
		} else if (msg instanceof ServerOkMessage) {
			out.writeBytes(OK);
		} else if (msg instanceof ServerPingMessage) {
			out.writeBytes(PING);
		} else if (msg instanceof ServerPongMessage) {
			out.writeBytes(PONG);
		} else {
			throw new NatsException("Unable to encode server of type " + msg.getClass().getName());
		}
	}

}
