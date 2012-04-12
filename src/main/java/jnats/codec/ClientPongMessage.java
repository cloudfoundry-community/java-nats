package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ClientPongMessage extends AbstractPong implements ClientMessage {
	public static final ClientPongMessage PONG_MESSAGE = new ClientPongMessage();
}
