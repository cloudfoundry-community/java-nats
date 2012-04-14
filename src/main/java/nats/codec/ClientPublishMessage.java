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
public class ClientPublishMessage implements ClientMessage, ClientRequest {

	private static final String CMD_PUBLISH = "PUB";

	private final String subject;

	private final String message;

	private final String replyTo;

	public ClientPublishMessage(String subject, String message, String replyTo) {
		// TODO Validate subject, message and replyTo What is valid?
		this.subject = subject;
		this.message = message;
		this.replyTo = replyTo;
	}

	public String getMessage() {
		return message;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public String getSubject() {
		return subject;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_PUBLISH).append(' ').append(subject).append(' ');
		if (replyTo != null) {
			builder.append(replyTo).append(' ');
		}
		builder.append(message.length()).append("\r\n").append(message).append("\r\n");
		return builder.toString();
	}
}
