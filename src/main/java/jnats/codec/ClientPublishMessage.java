package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ClientPublishMessage implements ClientMessage, ClientRequest {

	private static final String CMD_PUBLISH = "PUB";

	private final String subject;

	private final String message;

	private final String replyTo;

	public ClientPublishMessage(String subject, String message, String replyTo) {
		// TODO Validate subject, message and replyTo What is valid?
		this.subject = subject;
		this.message = message;
		this.replyTo = replyTo;
	}

	public String getMessage() {
		return message;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public String getSubject() {
		return subject;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_PUBLISH).append(' ').append(subject).append(' ');
		if (replyTo != null) {
			builder.append(replyTo).append(' ');
		}
		builder.append(message.length()).append("\r\n").append(message).append("\r\n");
		return builder.toString();
	}
}
