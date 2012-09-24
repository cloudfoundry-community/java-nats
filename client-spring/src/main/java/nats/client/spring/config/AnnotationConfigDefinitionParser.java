package nats.client.spring.config;

import nats.client.spring.AnnotationConfigBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class AnnotationConfigDefinitionParser implements BeanDefinitionParser {
	private static final String ATTRIBUTE_NATS_REF = "nats-ref";

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AnnotationConfigBeanPostProcessor.class);

		builder.addConstructorArgReference(element.getAttribute(ATTRIBUTE_NATS_REF));

		final BeanDefinition beanDefinition = builder.getBeanDefinition();
		parserContext.getReaderContext().registerWithGeneratedName(beanDefinition);
		return beanDefinition;
	}
}
