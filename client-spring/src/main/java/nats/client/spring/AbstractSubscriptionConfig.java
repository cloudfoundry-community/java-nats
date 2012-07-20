package nats.client.spring;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AbstractSubscriptionConfig {
	protected final Object bean;
	protected final String methodName;
	protected final String queueGroup;

	public AbstractSubscriptionConfig(Object bean, String methodName, String queueGroup) {
		this.bean = bean;
		this.methodName = methodName;
		this.queueGroup = queueGroup;
	}

	public Object getBean() {
		return bean;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getQueueGroup() {
		return queueGroup;
	}
}
