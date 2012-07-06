package nats.vcap;

import nats.NatsException;
import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import nats.client.Subscription;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

import java.io.IOException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsVcap {

	private final Nats nats;
	private final ObjectMapper mapper = new ObjectMapper();

	public NatsVcap(Nats nats) {
		this.nats = nats;
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	// TODO Implement me
	public void publish(VcapMessage message) {
	}

	// TODO Add support for queue groups and max messages.
	public <T extends VcapMessage> Subscription subscribe(final Class<T> type, final VcapMessageHandler<T> handler) {
		final Subscription subscribe = nats.subscribe(getSubject(type));
		final ObjectReader reader = mapper.reader(type);
		subscribe.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				final String body = message.getBody();
				try {
					final T vcapMessage = reader.readValue(body);
					handler.onMessage(message, vcapMessage);
				} catch (IOException e) {
					throw new NatsException(e);
				}

			}
		});
		return subscribe;
	}

	private <T extends VcapMessage> String getSubject(Class<T> type) {
		final NatsSubject subject = type.getAnnotation(NatsSubject.class);
		return subject.value();
	}
}
