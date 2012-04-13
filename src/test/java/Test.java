import jnats.CompletionHandler;
import jnats.client.Message;
import jnats.client.MessageHandler;
import jnats.client.Nats;
import jnats.NatsFuture;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Test {
	public static void main(String[] args) throws Exception {
		final Nats nats = new Nats.Builder()
//				.addHost(new URI("nats://nats:nats@api.vcap.ldschurch.org"))
				.addHost("nats://nats:nats@localhost")
//				.addHost("nats://nats:efcc5ac8e44ee5b4@api.mikeheath.cloudfoundry.me")
				.connect();
		nats.subscribe(">").addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				System.out.println("** onMessage ** " + message);
			}
		});
		nats.publish("foo", "message").addCompletionHandler(new CompletionHandler() {
			@Override
			public void onComplete(NatsFuture future) {
				System.out.println("Published message! " + future.isSuccess());
			}
		});
		//nats.close();
	}
}
