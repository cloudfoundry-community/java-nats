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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientPublishMessage implements ClientMessage, ClientRequest {

	private final String subject;

	private final String replyTo;

	private String body;

	public ClientPublishMessage(String subject, String replyTo) {
		this(subject, null, replyTo);
	}

	public ClientPublishMessage(String subject, String body, String replyTo) {
		// TODO Validate subject, replyTo What is valid?
		this.subject = subject;
		this.replyTo = replyTo;
		setBody(body);
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public String getSubject() {
		return subject;
	}

}
