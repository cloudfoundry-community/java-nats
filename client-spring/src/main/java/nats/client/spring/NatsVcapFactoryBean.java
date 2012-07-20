package nats.client.spring;

import nats.NatsException;
import nats.client.Message;
import nats.client.Nats;
import nats.vcap.NatsVcap;
import nats.vcap.VcapMessage;
import nats.vcap.VcapMessageHandler;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsVcapFactoryBean implements FactoryBean<NatsVcap> {

	private NatsVcap vcap;

	private Nats nats;
	private Collection<VcapSubscriptionConfig> subscriptions;

	@Override
	public NatsVcap getObject() throws Exception {
		if (vcap == null) {
			vcap = new NatsVcap(nats);

			for (VcapSubscriptionConfig subscription : subscriptions) {
				final Class<VcapMessage> type = subscription.getType();
				final Object bean = subscription.getBean();
				final String methodName = subscription.getMethodName();
				final Method method = type.getMethod(methodName, VcapMessage.class);
				vcap.subscribe(type, new VcapMessageHandler<VcapMessage>() {
					@Override
					public void onMessage(VcapMessage message) {
						try {
							method.invoke(bean, message);
						} catch (IllegalAccessException e) {
							throw new Error(e);
						} catch (InvocationTargetException e) {
							throw new NatsException(e.getTargetException());
						}
					}
				});
			}
		}
		return vcap;
	}

	@Override
	public Class<?> getObjectType() {
		return vcap == null ? NatsVcap.class : vcap.getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setNats(Nats nats) {
		this.nats = nats;
	}

	public void setSubscriptions(Collection<VcapSubscriptionConfig> subscriptions) {
		this.subscriptions = subscriptions;
	}
}
