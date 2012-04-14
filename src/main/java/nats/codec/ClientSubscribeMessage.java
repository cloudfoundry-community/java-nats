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
public class ClientSubscribeMessage implements ClientMessage, ClientRequest {

	private static final String CMD_SUBSCRIBE = "SUB";

	private final int id;

	private final String subject;

	private final String queueGroup;

	public ClientSubscribeMessage(int id, String subject) {
		this(id, subject, null);
	}

	public ClientSubscribeMessage(int id, String subject, String queueGroup) {
		// TODO Validate subject and queueGroup -- If they have white space it will break the protocol -- What is valid? -- Can't be empty. subject also has wild cards which must be valid.
		this.id = id;
		this.queueGroup = queueGroup;
		this.subject = subject;
	}

	public int getId() {
		return id;
	}

	public String getQueueGroup() {
		return queueGroup;
	}

	public String getSubject() {
		return subject;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_SUBSCRIBE).append(' ').append(subject).append(' ');
		if (queueGroup != null) {
			builder.append(queueGroup).append(' ');
		}
		builder.append(id).append("\r\n");
		return builder.toString();
	}
}
