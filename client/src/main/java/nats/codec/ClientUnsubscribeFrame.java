/*
 *   Copyright (c) 2012,2013 Mike Heath.  All rights reserved.
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
public class ClientUnsubscribeFrame implements ClientFrame {

	private final String id;

	private final Integer maxMessages;

	public ClientUnsubscribeFrame(String id) {
		this(id, null);
	}

	public ClientUnsubscribeFrame(String id, Integer maxMessages) {
		this.id = id;
		this.maxMessages = maxMessages;
	}

	public String getId() {
		return id;
	}

	public Integer getMaxMessages() {
		return maxMessages;
	}

}
