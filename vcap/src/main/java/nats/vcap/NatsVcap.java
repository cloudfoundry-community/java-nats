package nats.vcap;

import nats.NatsException;
import nats.client.Message;
import nats.client.MessageHandler;
import nats.client.Nats;
import nats.client.Publication;
import nats.client.Subscription;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

	public Publication publish(Object message) {
		final String subject = getSubject(message.getClass());
		final String encoding = encode(message);
		return nats.publish(subject, encoding);
	}

	public <T> Subscription subscribe(Class<T> type, VcapMessageHandler<T> handler) {
		return subscribe(type, null, null, handler);
	}

	public <T> Subscription subscribe(Class<T> type, Integer maxMessages, VcapMessageHandler<T> handler) {
		return subscribe(type, null, maxMessages, handler);
	}

	public <T> Subscription subscribe(Class<T> type, String queueGroup, VcapMessageHandler<T> handler) {
		return subscribe(type, queueGroup, null, handler);
	}

	public <T> Subscription subscribe(final Class<T> type, String queueGroup, Integer maxMessages, final VcapMessageHandler<T> handler) {
		final Subscription subscribe = nats.subscribe(getSubject(type), queueGroup, maxMessages);
		final ObjectReader reader = mapper.reader(type);
		subscribe.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(final Message message) {
				final String body = message.getBody();
				try {
					final T vcapMessage = reader.readValue(body);
					handler.onMessage(new VcapMessage<T>() {
						@Override
						public Message getNatsMessage() {
							return message;
						}

						@Override
						public T getMessage() {
							return vcapMessage;
						}

						@Override
						public Publication reply(Object replyMessage) {
							return message.reply(encode(replyMessage));
						}

						@Override
						public Publication reply(Object replyMessage, long delay, TimeUnit unit) {
							return message.reply(encode(replyMessage), delay, unit);
						}
					});
				} catch (IOException e) {
					throw new NatsException(e);
				}

			}
		});
		return subscribe;
	}

	private String encode(Object value) {
		try {
			return mapper.writeValueAsString(value);
		} catch (IOException e) {
			throw new NatsException(e);
		}
	}

	private String getSubject(Class<?> type) {
		final NatsSubject subject = type.getAnnotation(NatsSubject.class);
		return subject.value();
	}
}
