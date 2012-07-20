package nats.client.spring;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class SubscriptionConfig extends AbstractSubscriptionConfig {

	private final String subscription;

	public SubscriptionConfig(String subscription, Object bean, String methodName, String queueGroup) {
		super(bean, methodName, queueGroup);
		this.subscription = subscription;
	}

	public String getSubscription() {
		return subscription;
	}
}
