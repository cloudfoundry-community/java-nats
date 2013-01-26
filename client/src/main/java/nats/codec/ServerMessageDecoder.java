package nats.codec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerMessageDecoder extends AbstractMessageDecoder<ServerMessage> {

	// Regular expressions used for parsing server messages
	private static final Pattern MSG_PATTERN = Pattern.compile("^MSG\\s+(\\S+)\\s+(\\S+)\\s+((\\S+)[^\\S\\r\\n]+)?(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OK_PATTERN = Pattern.compile("^\\+OK\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern ERR_PATTERN = Pattern.compile("^-ERR\\s+('.+')?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PING_PATTERN = Pattern.compile("^PING", Pattern.CASE_INSENSITIVE);
	private static final Pattern PONG_PATTERN = Pattern.compile("^PONG", Pattern.CASE_INSENSITIVE);
	private static final Pattern INFO_PATTERN = Pattern.compile("^INFO\\s+([^\\r\\n]+)", Pattern.CASE_INSENSITIVE);

	private ServerPublishMessage message;

	public ServerMessageDecoder() {
		super();
	}

	public ServerMessageDecoder(int maxMessageSize) {
		super(maxMessageSize);
	}

	@Override
	protected ServerMessage handleBody(String body) {
		message.setBody(body);
		return message;
	}

	protected ServerMessage decodeCommand(String command) {
		Matcher matcher = MSG_PATTERN.matcher(command);
		if (matcher.matches()) {
			final String subject = matcher.group(1);
			final String id = matcher.group(2);
			// TODO Verify that this is really the queue group -- I don't think it is
			final String queueGroup = matcher.group(3);
			final String replyTo = matcher.group(4);
			final int length = Integer.valueOf(matcher.group(5));
			message = new ServerPublishMessage(id, subject, queueGroup, replyTo);
			expectBody(length);
			return null;
		}
		matcher = INFO_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerInfoMessage(matcher.group(1));
		}
		matcher = OK_PATTERN.matcher(command);
		if (matcher.matches()) {
			return ServerOkMessage.OK_MESSAGE;
		}
		matcher = ERR_PATTERN.matcher(command);
		if (matcher.matches()) {
			return new ServerErrorMessage(matcher.group(1));
		}
		if (PING_PATTERN.matcher(command).matches()) {
			return ServerPingMessage.PING;
		}
		if (PONG_PATTERN.matcher(command).matches()) {
			return ServerPongMessage.PONG;
		}
		throw new NatsDecodingException(command);
	}
}
