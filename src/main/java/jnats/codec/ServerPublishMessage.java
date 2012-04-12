package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ServerPublishMessage implements ServerMessage {

	private final int id;
	private final String subject;
	private final String queueGroup;
	private final String replyTo;
	private String body;

	public ServerPublishMessage(int id, String subject, String queueGroup, String replyTo) {
		this.id = id;
		this.subject = subject;
		this.queueGroup = queueGroup;
		this.replyTo = replyTo;
	}

	public int getId() {
		return id;
	}

	public String getQueueGroup() {
		return queueGroup;
	}

	public String getReplyTo() {
		return replyTo;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
