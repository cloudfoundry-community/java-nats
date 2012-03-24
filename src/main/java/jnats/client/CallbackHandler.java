package jnats.client;

import java.net.SocketAddress;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CallbackHandler implements Callback {
	@Override
	public void onConnect() {
	}

	@Override
	public void onClose() {
	}

	@Override
	public void onException(Throwable t) {
		t.printStackTrace();
	}

	@Override
	public void onServerReconnectFailed(SocketAddress address) {
	}

	@Override
	public void onServerConnectFailed() {
	}
}
