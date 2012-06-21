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
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientUnsubscribeMessage implements ClientMessage, ClientRequest {

	public static final String CMD_UNSUBSCRIBE = "UNSUB";

	private final String id;

	private final Integer maxMessages;

	public ClientUnsubscribeMessage(String id) {
		this(id, null);
	}

	public ClientUnsubscribeMessage(String id, Integer maxMessages) {
		this.id = id;
		this.maxMessages = maxMessages;
	}

	public String getId() {
		return id;
	}

	public Integer getMaxMessages() {
		return maxMessages;
	}

	@Override
	public ChannelBuffer encode() {
		final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(CMD_UNSUBSCRIBE.getBytes());
		buffer.writeByte(' ');
		buffer.writeBytes(id.getBytes());
		if (maxMessages != null) {
			buffer.writeByte(' ');
			ChannelBufferUtil.writeIntegerAsString(buffer, maxMessages);
		}
		buffer.writeBytes(ChannelBufferUtil.CRLF);
		return buffer;
	}
}
