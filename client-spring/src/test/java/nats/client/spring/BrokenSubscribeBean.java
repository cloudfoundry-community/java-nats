package nats.client.spring;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class BrokenSubscribeBean {

	/**
	 * This subscribe method is broken because it does not have a nats.client.Message parameter.
	 */
	@Subscribe("test.subject")
	public void subscribe() {
	}

}
