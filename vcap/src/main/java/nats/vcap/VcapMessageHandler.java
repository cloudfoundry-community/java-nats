package nats.vcap;

import nats.client.Message;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface VcapMessageHandler<T extends VcapMessage> {

	void onMessage(Message message, T vcapMessage);

}
