package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
class AbstractPong {
	public String encode() {
		return "PONG\r\n";
	}
}
