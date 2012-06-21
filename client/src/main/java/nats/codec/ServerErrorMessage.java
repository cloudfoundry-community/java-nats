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
public class ServerErrorMessage implements ServerMessage {

	public static final String CMD_ERR = "-ERR";
	private static final ChannelBuffer ERROR_BUFFER = ChannelBufferUtil.directBuffer(CMD_ERR + "\r\n");

	private final String errorMessage;

	public ServerErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public ChannelBuffer encode() {
		if (errorMessage == null) {
			return ERROR_BUFFER;
		}
		final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeBytes(CMD_ERR.getBytes());
		buffer.writeByte(' ');
		buffer.writeBytes(errorMessage.getBytes());
		buffer.writeBytes(ChannelBufferUtil.CRLF);
		return buffer;
	}
}
