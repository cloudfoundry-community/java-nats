package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ServerErrorMessage {

	private final String errorMessage;

	public ServerErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
