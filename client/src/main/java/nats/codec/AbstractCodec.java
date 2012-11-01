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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import java.nio.charset.Charset;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
abstract class AbstractCodec extends FrameDecoder {

	private static final ChannelBuffer DELIMITER = ChannelBuffers.wrappedBuffer(new byte[]{'\r', '\n'});

	protected enum State {COMMAND, BODY}

	private State state = State.COMMAND;
	private int bodyLength = 0;

	private final int maxMessageSize;

	protected AbstractCodec() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	protected AbstractCodec(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	protected void expectBody(int length) {
		bodyLength = length;
		state = State.BODY;
	}

	@Override
	protected final Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		switch (state) {
			case COMMAND:
				int frameLength = indexOf(buffer, DELIMITER);
				if (frameLength >= 0) {
					if (frameLength > maxMessageSize) {
						buffer.skipBytes(frameLength + DELIMITER.capacity());
						throwTooLongFrameException(ctx);
					} else {
						String command = buffer.readBytes(frameLength).toString(Charset.defaultCharset());
						buffer.skipBytes(DELIMITER.capacity());
						return decodeCommand(command);
					}
				}
				break;
			case BODY:
				if (buffer.readableBytes() >= bodyLength + DELIMITER.capacity()) {
					String body = buffer.readBytes(bodyLength).toString(Charset.defaultCharset());
					buffer.skipBytes(DELIMITER.capacity());
					state = State.COMMAND;
					return handleBody(body);
				}
				break;
			default:
				throw new Error("Unknown state: " + state);
		}
		if (buffer.readableBytes() > maxMessageSize) {
			throwTooLongFrameException(ctx);
		}
		return null;
	}

	protected abstract Object handleBody(String body);

	protected abstract Object decodeCommand(String command);

	private void throwTooLongFrameException(ChannelHandlerContext ctx) {
		Channels.fireExceptionCaught(
				ctx.getChannel(),
				new TooLongFrameException("message size exceeds " + maxMessageSize));
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 * <p/>
	 * Copied from {@link org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder}.
	 */
	private int indexOf(ChannelBuffer haystack, ChannelBuffer needle) {
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
