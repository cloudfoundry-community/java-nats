package nats.codec;

import nats.NatsException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsDecodingException extends NatsException {

	private final String command;

	public NatsDecodingException(String command) {
		super("Don't know how to handle the following sent by the Nats server: " + command);
		this.command = command;
	}

	public String getCommand() {
		return command;
	}
}
