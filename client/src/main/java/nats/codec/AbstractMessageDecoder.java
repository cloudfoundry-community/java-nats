package nats.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import nats.Constants;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
abstract class AbstractMessageDecoder<T extends NatsMessage> extends ByteToMessageDecoder<T> {

	private static final ByteBuf DELIMITER = Unpooled.copiedBuffer(new byte[]{'\r', '\n'});

	protected enum State {COMMAND, BODY}

	private State state = State.COMMAND;
	private int bodyLength = 0;

	private final int maxMessageSize;

	protected AbstractMessageDecoder() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	protected AbstractMessageDecoder(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	protected void expectBody(int length) {
		bodyLength = length;
		state = State.BODY;
	}

	@Override
	public final T decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		switch (state) {
			case COMMAND:
				int frameLength = indexOf(in, DELIMITER);
				if (frameLength >= 0) {
					if (frameLength > maxMessageSize) {
						in.skipBytes(frameLength + DELIMITER.capacity());
						throwTooLongFrameException(ctx);
					} else {
						String command = in.readBytes(frameLength).toString(CharsetUtil.UTF_8);
						in.skipBytes(DELIMITER.capacity());
						return decodeCommand(command);
					}
				}
				break;
			case BODY:
				if (in.readableBytes() >= bodyLength + DELIMITER.capacity()) {
					String body = in.readBytes(bodyLength).toString(CharsetUtil.UTF_8);
					in.skipBytes(DELIMITER.capacity());
					state = State.COMMAND;
					return handleBody(body);
				}
				break;
			default:
				throw new Error("Unknown state: " + state);
		}
		if (in.readableBytes() > maxMessageSize) {
			throwTooLongFrameException(ctx);
		}
		return null;
	}

	protected abstract T handleBody(String body);

	protected abstract T decodeCommand(String command);

	private void throwTooLongFrameException(ChannelHandlerContext ctx) {
		ctx.fireExceptionCaught(new TooLongFrameException("message size exceeds " + maxMessageSize));
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 * <p/>
	 * Copied from {@link io.netty.handler.codec.DelimiterBasedFrameDecoder}.
	 */
	private int indexOf(ByteBuf haystack, ByteBuf needle) {
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
