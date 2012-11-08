package nats.codec;

import io.netty.channel.CombinedChannelHandler;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodec extends CombinedChannelHandler {

	public ClientCodec(int maxMessageSize) {
		init(new ServerMessageDecoder(maxMessageSize), new ClientMessageEncoder());
	}
}
