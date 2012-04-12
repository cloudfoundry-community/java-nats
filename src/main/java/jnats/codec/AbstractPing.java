package jnats.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
class AbstractPing {
	public String encode() {
		return "PING\r\n";
	}
}
