import nats.CompletionHandler;
import nats.NatsLogger;
import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import nats.NatsFuture;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Test {
	public static void main(String[] args) throws Exception {
		final Nats nats = new Nats.Builder()
//				.addHost("nats://nats:nats@api.vcap.ldschurch.org")
				.addHost("nats://nats:nats@localhost")
//				.addHost("nats://nats:efcc5ac8e44ee5b4@api.mikeheath.cloudfoundry.me")
//				.logger(NatsLogger.DEBUG_LOGGER)
				.connect();
//		nats.subscribe(">").addMessageHandler(new MessageHandler() {
//			@Override
//			public void onMessage(Message message) {
//				System.out.println(message);
//			}
//		});
		for(Message message : nats.subscribe(">")) {
			System.out.println(message);
		}
//		nats.publish("foo", "message").addCompletionHandler(new CompletionHandler() {
//			@Override
//			public void onComplete(NatsFuture future) {
//				System.out.println("Published message! " + future.isSuccess());
//			}
//		});
		//nats.close();
	}
}
