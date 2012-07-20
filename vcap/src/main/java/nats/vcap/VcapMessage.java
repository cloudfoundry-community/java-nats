package nats.vcap;

import nats.client.Message;
import nats.client.Publication;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface VcapMessage<T> {

	Message getNatsMessage();

	T getMessage();

	Publication reply(Object message);

	Publication reply(Object message, long delay, TimeUnit unit);

}
