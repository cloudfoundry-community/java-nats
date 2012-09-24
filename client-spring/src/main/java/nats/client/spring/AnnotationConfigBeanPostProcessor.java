package nats.client.spring;

import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AnnotationConfigBeanPostProcessor implements BeanPostProcessor {

	private final Nats nats;

	public AnnotationConfigBeanPostProcessor(Nats nats) {
		this.nats = nats;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		final Class<?> clazz = bean.getClass();
		for (final Method method : clazz.getMethods()) {
			final Subscribe annotation = AnnotationUtils.findAnnotation(method, Subscribe.class);
			if (annotation != null) {
				final Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length != 1 || !parameterTypes[0].equals(Message.class)) {
					throw new BeanInitializationException(String.format(
							"Method '%s' on bean with name '%s' must have a single parameter of type %s when using the @%s annotation.",
							method.toGenericString(),
							beanName,
							Message.class.getName(),
							Subscribe.class.getName()
							));
				}
				nats.subscribe(annotation.value()).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						try {
							method.invoke(bean, message);
						} catch (IllegalAccessException e) {
							throw new RuntimeException(e);
						} catch (InvocationTargetException e) {
							final Throwable targetException = e.getTargetException();
							if (targetException instanceof RuntimeException) {
								throw (RuntimeException)targetException;
							}
							throw new RuntimeException(targetException);
						}
					}
				});
			}
		}

		return bean;
	}
}
