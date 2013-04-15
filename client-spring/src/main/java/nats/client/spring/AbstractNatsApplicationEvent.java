/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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
package nats.client.spring;

import nats.client.Nats;
import org.springframework.context.ApplicationEvent;

/**
 * Abstract application event class for NATS events.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class AbstractNatsApplicationEvent extends ApplicationEvent {
	private final Nats nats;

	public AbstractNatsApplicationEvent(Nats nats) {
		super(nats);
		this.nats = nats;
	}

	public Nats getNats() {
		return nats;
	}

	@Override
	public Nats getSource() {
		return (Nats)super.getSource();
	}
}
