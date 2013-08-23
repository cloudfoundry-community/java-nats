/*
 *   Copyright (c) 2013 Intellectual Reserve, Inc.  All rights reserved.
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

import nats.client.ConnectionStateListener;
import nats.client.Nats;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publishes NATS connection state change events as Spring events from NatsFactoryBean.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ApplicationEventPublishingConnectionStateListener implements ConnectionStateListener  {

	private final ApplicationEventPublisher applicationEventPublisher;

	public ApplicationEventPublishingConnectionStateListener(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void onConnectionStateChange(Nats nats, State state) {
		switch (state) {
			case CONNECTED:
				applicationEventPublisher.publishEvent(new NatsConnectedApplicationEvent(nats));
				break;
			case DISCONNECTED:
				applicationEventPublisher.publishEvent(new NatsClosedApplicationEvent(nats));
				break;
			case SERVERY_READY:
				applicationEventPublisher.publishEvent(new NatsServerReadyApplicationEvent(nats));
				break;
		}
	}
}
