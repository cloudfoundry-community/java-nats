package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ClientUnsubscribeMessage implements ClientMessage, ClientRequest {

	private static final String CMD_UNSUBSCRIBE = "UNSUB";

	private final int id;

	private final Integer maxMessages;

	public ClientUnsubscribeMessage(int id) {
		this(id, null);
	}

	public ClientUnsubscribeMessage(int id, Integer maxMessages) {
		this.id = id;
		this.maxMessages = maxMessages;
	}

	public int getId() {
		return id;
	}

	public Integer getMaxMessages() {
		return maxMessages;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_UNSUBSCRIBE).append(' ').append(id);
		if (maxMessages != null) {
			builder.append(' ').append(maxMessages);
		}
		builder.append("\r\n");
		return builder.toString();
	}
}
