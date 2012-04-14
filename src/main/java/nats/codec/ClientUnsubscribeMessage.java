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

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientUnsubscribeMessage implements ClientMessage, ClientRequest {

	private static final String CMD_UNSUBSCRIBE = "UNSUB";

	private final int id;

	private final Integer maxMessages;

	public ClientUnsubscribeMessage(int id) {
		this(id, null);
	}

	public ClientUnsubscribeMessage(int id, Integer maxMessages) {
		this.id = id;
		this.maxMessages = maxMessages;
	}

	public int getId() {
		return id;
	}

	public Integer getMaxMessages() {
		return maxMessages;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_UNSUBSCRIBE).append(' ').append(id);
		if (maxMessages != null) {
			builder.append(' ').append(maxMessages);
		}
		builder.append("\r\n");
		return builder.toString();
	}
}
