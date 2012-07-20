package nats.client.spring;

import nats.vcap.VcapMessage;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class VcapSubscriptionConfig extends AbstractSubscriptionConfig {

	private final Class<VcapMessage> type;

	public VcapSubscriptionConfig(Class<VcapMessage> type, Object bean, String methodName, String queueGroup) {
		super(bean, methodName, queueGroup);
		this.type = type;
	}

	public Class<VcapMessage> getType() {
		return type;
	}
}
