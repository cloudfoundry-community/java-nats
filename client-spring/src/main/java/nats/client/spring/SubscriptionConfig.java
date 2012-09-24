package nats.client.spring;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class SubscriptionConfig {

	private final Object bean;
	private final String methodName;
	private final String queueGroup;
	private final String subscription;

	public SubscriptionConfig(String subscription, Object bean, String methodName, String queueGroup) {
		this.subscription = subscription;
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

	public String getSubscription() {
		return subscription;
	}

}
