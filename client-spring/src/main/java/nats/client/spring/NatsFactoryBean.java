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

import io.netty.channel.EventLoopGroup;
import nats.NatsException;
import nats.client.ConnectionStateListener;
import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import nats.client.NatsConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsFactoryBean implements FactoryBean<Nats>, DisposableBean, ApplicationEventPublisherAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(NatsFactoryBean.class);

	private Nats nats;

	private Collection<String> hostUris;
	private boolean autoReconnect = true;
	private EventLoopGroup eventLoopGroup;
	private ConnectionStateListener connectionStateListener;
	private long reconnectWaitTime = -1;
	private Executor callbackExecutor;

	private Collection<SubscriptionConfig> subscriptions;

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public Nats getObject() throws Exception {
		if (nats != null) {
			return nats;
		}
		LOGGER.debug("Creating NATS client");
		final NatsBuilder connector = new NatsBuilder(applicationEventPublisher);
		if (hostUris == null) {
			throw new IllegalStateException("At least one host URI must be provided.");
		}
		for (String uri : hostUris) {
			connector.addHost(uri);
		}
		connector.automaticReconnect(autoReconnect);
		if (connectionStateListener != null) {
			connector.addConnectionStateListener(connectionStateListener);
		}
		if (callbackExecutor != null) {
			connector.calllbackExecutor(callbackExecutor);
		}

		if (eventLoopGroup != null) {
			connector.eventLoopGroup(eventLoopGroup);
		}
		if (reconnectWaitTime >= 0) {
			connector.reconnectWaitTime(reconnectWaitTime, TimeUnit.MILLISECONDS);
		}
		nats = connector.connect();
		for (SubscriptionConfig subscription : subscriptions) {
			final Object bean = subscription.getBean();
			final Method method = bean.getClass().getMethod(subscription.getMethodName(), Message.class);
			nats.subscribe(subscription.getSubscription(), subscription.getQueueGroup()).addMessageHandler(new MessageHandler() {
				@Override
				public void onMessage(Message message) {
					try {
						method.invoke(bean, message);
					} catch (IllegalAccessException e) {
						throw new Error(e);
					} catch (InvocationTargetException e) {
						throw new NatsException(e.getTargetException());
					}
				}
			});
		}
		return nats;
	}

	@Override
	public Class<?> getObjectType() {
		return nats == null ? Nats.class : nats.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		if (nats != null) {
			LOGGER.debug("Closing NATS client.");
			nats.close();
		}
	}

	public void setHostUris(Collection<String> hostUris) {
		this.hostUris = new ArrayList<>(hostUris);
	}

	public void setAutoReconnect(boolean autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
		this.connectionStateListener = connectionStateListener;
	}

	public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
	}

	public void setReconnectWaitTime(long reconnectWaitTime) {
		this.reconnectWaitTime = reconnectWaitTime;
	}

	public void setSubscriptions(Collection<SubscriptionConfig> subscriptions) {
		this.subscriptions = subscriptions;
	}

	public void setCallbackExecutor(Executor callbackExecutor) {
		this.callbackExecutor = callbackExecutor;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
