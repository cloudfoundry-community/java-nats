package nats.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerCodec  extends AbstractCodec {

	private ClientPublishMessage message;

	public ServerCodec() {
		super();
	}

	@Override
	protected ClientMessage decodeCommand(String command) {
		// CONNECT
		if (command.startsWith(ClientConnectMessage.CMD_CONNECT)) {
			final String body = command.substring(ClientConnectMessage.CMD_CONNECT.length()).trim();
			final ConnectBody connectBody = ConnectBody.parse(body);
			return new ClientConnectMessage(connectBody);
		}

		// PING
		if (ClientPingMessage.CMD_PING.equals(command.trim())) {
			return ClientPingMessage.PING;
		}

		// PONG
		if (ClientPongMessage.CMD_PONG.equals(command.trim())) {
			return ClientPongMessage.PONG;
		}

		// PUB
		if (command.startsWith(ClientPublishMessage.CMD_PUBLISH)) {
			try {
				final String[] parts = command.substring(ClientPublishMessage.CMD_PUBLISH.length()).trim().split("\\s+");
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
		if (command.startsWith(ClientSubscribeMessage.CMD_SUBSCRIBE)) {
			final String[] parts = command.substring(ClientSubscribeMessage.CMD_SUBSCRIBE.length()).trim().split("\\s+");
			if (parts.length < 2 || parts.length > 3) {
				throw new NatsDecodingException(command);
			}
			final String subject = parts[0];
			final String id = parts[parts.length -1];
			final String queueGroup = (parts.length == 3) ? parts[1] : null;
			return new ClientSubscribeMessage(id, subject, queueGroup);
		}

		// UNSUB
		if (command.startsWith(ClientUnsubscribeMessage.CMD_UNSUBSCRIBE)) {
			final String[] parts = command.substring(ClientUnsubscribeMessage.CMD_UNSUBSCRIBE.length()).trim().split("\\s+");
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
