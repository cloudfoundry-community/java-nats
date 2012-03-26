package jnats.client;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public interface Message {

	Subscription getSubscription();

	String getSubject();

	String getBody();

	String getReplyTo();

	NatsFuture reply(String message);

	NatsFuture reply(String message, long delay, TimeUnit unit);

}
