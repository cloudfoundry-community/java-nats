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
package nats.client;

/**
 * Provides an abstract implementation of the {@link Message} interface.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractMessage implements Message {

	protected AbstractMessage(Subscription subscription, String subject, String body, String replyTo) {
		this.subscription = subscription;
		this.subject = subject;
		this.body = body;
		this.replyTo = replyTo;
	}

	private final Subscription subscription;
	private final String subject;
	private final String body;
	private final String replyTo;

	@Override
	public Subscription getSubscription() {
		return subscription;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public String getBody() {
		return body;
	}

	@Override
	public String getReplyTo() {
		return replyTo;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[subject: '").append(subject).append("', body: '").append(body).append("'");
		if (replyTo != null && replyTo.trim().length() > 0) {
			builder.append(", replyTo: '").append(replyTo).append("'");
		}
		builder.append(']');
		return builder.toString();
	}

}
