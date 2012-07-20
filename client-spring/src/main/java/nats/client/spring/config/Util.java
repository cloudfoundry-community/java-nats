package nats.client.spring.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class Util {

	static final String ATTRIBUTE_REF = "ref";
	static final String ATTRIBUTE_METHOD = "method";
	static final String ATTRIBUTE_QUEUE_GROUP = "queue-group";

	public static void parseSubscriptionConfigAttributes(Element subscriptionElement, BeanDefinitionBuilder subscriptionBuilder) {
		subscriptionBuilder.addConstructorArgReference(subscriptionElement.getAttribute(ATTRIBUTE_REF));
		subscriptionBuilder.addConstructorArgValue(subscriptionElement.getAttribute(ATTRIBUTE_METHOD));
		subscriptionBuilder.addConstructorArgValue(subscriptionElement.getAttribute(ATTRIBUTE_QUEUE_GROUP));
	}
}
