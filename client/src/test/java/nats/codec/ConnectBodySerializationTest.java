package nats.codec;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @author Mike Heath
 */
public class ConnectBodySerializationTest {

	@Test
	public void deserializeEmptyJson() {
		final ConnectBody body = ConnectBody.parse("{}");
		assertNotNull(body);
		assertNull(body.getPassword());
		assertNull(body.getUser());
		assertFalse(body.isPedantic());
		assertFalse(body.isVerbose());
	}

	@Test
	public void deserializeUnkownField() {
		final ConnectBody body = ConnectBody.parse("{\"yourmom\":\"goes to college\"}");
		assertNotNull(body);
	}

}
