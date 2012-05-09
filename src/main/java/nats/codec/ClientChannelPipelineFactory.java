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
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientChannelPipelineFactory implements ChannelPipelineFactory {

	public static final String PIPELINE_CODEC = "nats-codec";

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
		pipeline.addLast(PIPELINE_CODEC, new ClientCodec(maxMessageSize));
		return pipeline;
	}
}
