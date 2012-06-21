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
	static final String ATTRIBUTE_CHANNEL_FACTORY_REF = "channel-factory-ref";
	static final String ATTRIBUTE_EXCEPTION_HANDLER_REF = "exception-handler-ref";
	static final String ATTRIBUTE_LOGGER_REF = "logger-ref";
	static final String ATTRIBUTE_MAX_RECONNECT_ATTEMPTS = "max-reconnect-attempts";
	static final String ATTRIBUTE_RECONNECT_WAIT_TIME = "reconnect-wait-time";
	static final String ELEMENT_HOST = "host";

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NatsFactoryBean.class);

		final List<String> uris = new ManagedList<String>();
		final List<Element> hosts = DomUtils.getChildElementsByTagName(element, ELEMENT_HOST);
		for (Element host : hosts) {
			uris.add(host.getTextContent());
		}
		builder.addPropertyValue("hostUris", uris);

		builder.addPropertyValue("autoReconnect", element.getAttribute(ATTRIBUTE_AUTO_RECONNECT));
		builder.addPropertyValue("maxReconnectAttempts", element.getAttribute(ATTRIBUTE_MAX_RECONNECT_ATTEMPTS));
		builder.addPropertyValue("reconnectWaitTime", element.getAttribute(ATTRIBUTE_RECONNECT_WAIT_TIME));
		final String channelFactoryRef = element.getAttribute(ATTRIBUTE_CHANNEL_FACTORY_REF);
		if (StringUtils.hasText(channelFactoryRef)) {
			builder.addPropertyReference("channelFactory", channelFactoryRef);
		}
		final String exceptionHandlerRef = element.getAttribute(ATTRIBUTE_EXCEPTION_HANDLER_REF);
		if (StringUtils.hasText(exceptionHandlerRef)) {
			builder.addPropertyReference("exceptionHandler", exceptionHandlerRef);
		}
		final String loggerRef = element.getAttribute(ATTRIBUTE_LOGGER_REF);
		if (StringUtils.hasText(loggerRef)) {
			builder.addPropertyReference("logger", loggerRef);
		}

		final String id = element.getAttribute(ATTRIBUTE_ID);

		final AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);

		return beanDefinition;
	}
}
