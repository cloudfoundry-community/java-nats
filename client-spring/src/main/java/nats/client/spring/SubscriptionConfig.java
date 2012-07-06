package nats.client.spring;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class SubscriptionConfig {

	private final String subscription;
	private final Object bean;
	private final String methodName;
	private final String queueGroup;

	public SubscriptionConfig(String subscription, Object bean, String methodName) {
		this(subscription, bean, methodName, null);
	}

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
