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
public class ClientSubscribeMessage implements ClientMessage, ClientRequest {

	private final String id;

	private final String subject;

	private final String queueGroup;

	public ClientSubscribeMessage(String id, String subject, String queueGroup) {
		// TODO Validate subject and queueGroup -- If they have white space it will break the protocol -- What is valid? -- Subject can't be empty. subject also has wild cards which must be valid.
		this.id = id;
		this.queueGroup = queueGroup;
		this.subject = subject;
	}

	public String getId() {
		return id;
	}

	public String getQueueGroup() {
		return queueGroup;
	}

	public String getSubject() {
		return subject;
	}

}
