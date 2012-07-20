package nats.vcap;

import nats.client.Message;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface VcapMessageHandler<T> {

	void onMessage(VcapMessage<T> message);

}
