package nats.codec;

import java.util.regex.Pattern;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientMessageDecoder extends AbstractMessageDecoder<ClientMessage> {

	public static final String CMD_CONNECT = "CONNECT";
	public static final String CMD_PUBLISH = "PUB";
	public static final String CMD_SUBSCRIBE = "SUB";
	public static final String CMD_UNSUBSCRIBE = "UNSUB";

	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);

	private ClientPublishMessage message;

	@Override
	protected ClientMessage decodeCommand(String command) {
		// CONNECT
		if (command.startsWith(CMD_CONNECT)) {
			final String body = command.substring(CMD_CONNECT.length()).trim();
			final ConnectBody connectBody = ConnectBody.parse(body);
			return new ClientConnectMessage(connectBody);
		}

		// PING
		if (PING_PATTERN.matcher(command).matches()) {
			return ClientPingMessage.PING;
		}

		// PONG
		if (PONG_PATTERN.matcher(command).matches()) {
			return ClientPongMessage.PONG;
		}

		// PUB
		if (command.startsWith(CMD_PUBLISH)) {
			try {
				final String[] parts = command.substring(CMD_PUBLISH.length()).trim().split("\\s+");
				if (parts.length < 2 || parts.length > 3) {
					throw new NatsDecodingException(command);
				}
				final String subject = parts[0];
				final int length = Integer.parseInt(parts[parts.length - 1]);
				final String replyTo = (parts.length == 3) ? parts[1] : null;
				this.message = new ClientPublishMessage(subject, replyTo);
				expectBody(length);
				return null;
			} catch (NumberFormatException e) {
				throw new NatsDecodingException(command);
			}
		}

		// SUB
		if (command.startsWith(CMD_SUBSCRIBE)) {
			final String[] parts = command.substring(CMD_SUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 2 || parts.length > 3) {
				throw new NatsDecodingException(command);
			}
			final String subject = parts[0];
			final String id = parts[parts.length - 1];
			final String queueGroup = (parts.length == 3) ? parts[1] : null;
			return new ClientSubscribeMessage(id, subject, queueGroup);
		}

		// UNSUB
		if (command.startsWith(CMD_UNSUBSCRIBE)) {
			final String[] parts = command.substring(CMD_UNSUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 1 || parts.length > 2) {
				throw new NatsDecodingException(command);
			}
			final String id = parts[0];
			final Integer maxMessages = (parts.length == 2) ? Integer.valueOf(parts[1]) : null;
			return new ClientUnsubscribeMessage(id, maxMessages);
		}

		throw new NatsDecodingException(command);
	}

	@Override
	protected ClientMessage handleBody(String body) {
		message.setBody(body);
		return message;
	}

}
