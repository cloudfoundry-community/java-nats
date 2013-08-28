package nats.client;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class DefaultMessageTest {

	@Test
	public void testGetters() {
		final String subject = "some.subject";
		final String body = "body";
		final String queueGroup = "group";
		final boolean request = false;

		final DefaultMessage message = new DefaultMessage(subject, body, queueGroup, request);

		assertEquals(message.getBody(), body);
		assertEquals(message.getQueueGroup(), queueGroup);
		assertEquals(message.getSubject(), subject);
		assertEquals(message.isRequest(), request);
	}

	@Test(expectedExceptions = UnsupportedOperationException.class)
	public void replyThrowsException() {
		new DefaultMessage("", "", "", false).reply("Reply body.");
	}

	@Test(expectedExceptions = UnsupportedOperationException.class)
	public void delayedReplyThrowsException() {
		new DefaultMessage("", "", "", false).reply("Reply body.", 1, TimeUnit.DAYS);
	}

}
