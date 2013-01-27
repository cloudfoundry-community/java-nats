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
package nats.client.spring.config;

import nats.client.spring.NatsFactoryBean;
import nats.client.spring.SubscriptionConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class NatsBeanDefinitionParser implements BeanDefinitionParser {

	static final String ATTRIBUTE_ID = "id";
	static final String ATTRIBUTE_AUTO_RECONNECT = "auto-reconnect";
	static final String ATTRIBUTE_EVENT_LOOP_GROUP_REF = "event-loop-group-ref";
	static final String ATTRIBUTE_CONNECTION_STATE_LISTENER_REF = "connection-state-listener-ref";
	static final String ATTRIBUTE_RECONNECT_WAIT_TIME = "reconnect-wait-time";
	static final String ELEMENT_URL = "url";

	static final String ELEMENT_SUBSCRIPTION = "subscription";
	static final String ATTRIBUTE_SUBJECT = "subject";
	static final String ATTRIBUTE_REF = "ref";
	static final String ATTRIBUTE_METHOD = "method";
	static final String ATTRIBUTE_QUEUE_GROUP = "queue-group";

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NatsFactoryBean.class);

		// Parse list of hosts
		final List<String> uris = new ManagedList<>();
		final List<Element> hosts = DomUtils.getChildElementsByTagName(element, ELEMENT_URL);
		for (Element host : hosts) {
			uris.add(host.getTextContent());
		}
		builder.addPropertyValue("hostUris", uris);

		// Parse list of subscriptions
		final List<BeanDefinition> subscriptions = new ManagedList<>();
		final List<Element> subscriptionElements = DomUtils.getChildElementsByTagName(element, ELEMENT_SUBSCRIPTION);
		for (Element subscriptionElement : subscriptionElements) {
			final BeanDefinitionBuilder subscriptionBuilder = BeanDefinitionBuilder.genericBeanDefinition(SubscriptionConfig.class);
			subscriptionBuilder.addConstructorArgValue(subscriptionElement.getAttribute(ATTRIBUTE_SUBJECT));
			subscriptionBuilder.addConstructorArgReference(subscriptionElement.getAttribute(ATTRIBUTE_REF));
			subscriptionBuilder.addConstructorArgValue(subscriptionElement.getAttribute(ATTRIBUTE_METHOD));
			subscriptionBuilder.addConstructorArgValue(subscriptionElement.getAttribute(ATTRIBUTE_QUEUE_GROUP));
			subscriptions.add(subscriptionBuilder.getBeanDefinition());
		}
		builder.addPropertyValue("subscriptions", subscriptions);

		// Parse attributes
		builder.addPropertyValue("autoReconnect", element.getAttribute(ATTRIBUTE_AUTO_RECONNECT));
		builder.addPropertyValue("reconnectWaitTime", element.getAttribute(ATTRIBUTE_RECONNECT_WAIT_TIME));
		final String eventLoopGroupRef = element.getAttribute(ATTRIBUTE_EVENT_LOOP_GROUP_REF);
		if (StringUtils.hasText(eventLoopGroupRef)) {
			builder.addPropertyReference("eventLoopGroup", eventLoopGroupRef);
		}
		final String connectionStateListenerRef = element.getAttribute(ATTRIBUTE_CONNECTION_STATE_LISTENER_REF);
		if (StringUtils.hasText(connectionStateListenerRef)) {
			builder.addPropertyReference("connectionStateListener", connectionStateListenerRef);
		}

		// Register bean
		final String id = element.getAttribute(ATTRIBUTE_ID);

		final AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);

		return beanDefinition;
	}

}
