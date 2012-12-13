import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import nats.client.NatsConnector;

public class SimpleClient {
	public static void main(String[] args) {
//		final String natsPassword = "4d100351123b0a78";
		final String natsPassword = "83dd6854a5482330";
		final String vmIpAddress = "172.16.149.129";
		Nats nats = new NatsConnector().maxReconnectAttempts(Integer.MAX_VALUE).addHost("nats://nats:"+ natsPassword + "@" + vmIpAddress).connect();

		nats.subscribe(">").addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				System.out.println(message.getSubject() + "\t" + message.getBody());
			}
		});

		nats.request("vcap.component.discover", new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				System.out.println(">>> " + message.getBody());
			}
		});
	}
}
