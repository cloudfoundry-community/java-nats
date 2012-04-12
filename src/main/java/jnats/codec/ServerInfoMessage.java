package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ServerInfoMessage implements ServerMessage {

	private final String info;

	public ServerInfoMessage(String info) {
		this.info = info;
	}

	public String getInfo() {
		return info;
	}
}
