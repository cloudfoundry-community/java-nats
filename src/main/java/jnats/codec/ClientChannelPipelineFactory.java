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

import jnats.Constants;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientChannelPipelineFactory implements ChannelPipelineFactory {

	public static final String PIPELINE_FRAME_DECODER = "frameDecoder";
	public static final String PIPELINE_STRING_DECODER = "stringDecoder";
	public static final String PIPELINE_STRING_ENCODER = "stringEncoder";
	public static final String PIPELINE_FIXED_DECODER = "fixedDecoder";
	public static final String PIPELINE_CODEC = "codec";

	private static final StringDecoder decoder = new StringDecoder();
	private static final StringEncoder encoder = new StringEncoder();

	/**
	 * The maximum message size this client will accept from a Nats server.
	 */
	private final int maxMessageSize;

	public ClientChannelPipelineFactory() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	public ClientChannelPipelineFactory(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public ChannelPipeline getPipeline() {
		final ChannelPipeline pipeline = Channels.pipeline();
		final DelimiterBasedFrameDecoder delimiterBasedFrameDecoder = new DelimiterBasedFrameDecoder(
				maxMessageSize,
				ChannelBuffers.wrappedBuffer(new byte[]{'\r', '\n'})
		);
		pipeline.addFirst(PIPELINE_FRAME_DECODER, delimiterBasedFrameDecoder);
		pipeline.addLast(PIPELINE_STRING_DECODER, decoder);
		pipeline.addLast(PIPELINE_STRING_ENCODER, encoder);
		pipeline.addLast("debug", new SimpleChannelHandler() {
			@Override
			public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
				super.writeRequested(ctx, e);
				System.out.println("Sent: " + e.getMessage());
			}

			@Override
			public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
				System.out.println("Received: " + e.getMessage());
				super.messageReceived(ctx, e);
			}
		});
		pipeline.addLast(PIPELINE_CODEC, new ClientCodec());
		return pipeline;
	}
}
