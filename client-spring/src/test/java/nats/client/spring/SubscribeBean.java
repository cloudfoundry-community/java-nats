package nats.client.spring;

import nats.client.Message;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class SubscribeBean {

	/**
	 * This subscribe method is broken because it does not have a nats.client.Message parameter.
	 */
	@Subscribe("test.subject")
	public void subscribe(Message message) {
		System.out.println(message);
	}

}
