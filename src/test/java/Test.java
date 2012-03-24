import jnats.client.CompletionHandler;
import jnats.client.MessageHandler;
import jnats.client.Nats;
import jnats.client.NatsFuture;
import jnats.client.Subscription;

import java.net.URI;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Test {
	public static void main(String[] args) throws Exception {
		final Nats nats = new Nats.Builder()
				.addHost(new URI("nats://nats:efcc5ac8e44ee5b4@api.mikeheath.cloudfoundry.me"))
				.addHost("nats://localhost").connect();
		//final Nats nats = new Nats.Builder().addHost(new URI("nats://localhost")).connect();
		Thread.sleep(200);
		nats.subscribe("foo").addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Subscription subscription, String message, String replyTo) {
				System.out.println("In handler: " + message);
			}
		});
		nats.publish("foo", "Hi there").addCompletionHandler(new CompletionHandler() {
			@Override
			public void onComplete(NatsFuture future) {
				System.out.println("Publish was sent.");
			}
		});
	}
}
